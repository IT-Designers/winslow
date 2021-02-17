package de.itdesigners.winslow.fs;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class LockBus {

    private static final Logger LOG = Logger.getLogger(LockBus.class.getSimpleName());

    public static final int    LOCK_RETRY_READ_COOLDOWN_MS   = 100;
    public static final int    LOCK_RETRY_READ_MAX_TRIALS    = 300; // with 100ms cooldown this is about 30s
    public static final int    LOCK_RETRY_MIN_THRESHOLD      = LOCK_RETRY_READ_MAX_TRIALS / 20;
    public static final int    LOCK_DURATION_OFFSET          = 1_000;
    public static final int    DURATION_SURELY_OUT_OF_DATE   = 5_000;
    public static final int    DURATION_FOR_UNREADABLE_FILES = 25_000;
    public static final int    MAX_OLD_EVENT_FILE_COUNT      = 25;
    public static final int    MIN_POLL_TIME_INTERVALL       = 10;
    public static final String COMMON_LOCK_FILE              = ".lock";

    private final String             name;
    private final Path               eventDirectory;
    private final Map<String, Event> locks = new HashMap<>();

    private final Map<Event.Command, List<Consumer<Event>>> listener = new EnumMap<>(Event.Command.class);

    private             int  eventCounter           = 0;
    public static final long MAX_POLL_TIME_INTERVAL = 1_000L;

    public LockBus(String name, Path eventDirectory) throws IOException, LockException {
        this.name           = name;
        this.eventDirectory = eventDirectory;
        if (!eventDirectory.toFile().exists() && !eventDirectory.toFile().mkdirs()) {
            throw new IOException("Failed to create event directory at: " + eventDirectory);
        }
        if (eventDirectory.toFile().exists() && !eventDirectory.toFile().isDirectory()) {
            throw new IOException("Path to event directory is not a directory: " + eventDirectory);
        }

        this.initEventCounter();
        this.ensureLocksAreUpToDate();
        this.startEventDirWatchService(eventDirectory);
    }

    public void registerEventListener(
            @Nonnull Event.Command command,
            @Nonnull Consumer<Event> consumer,
            RegistrationOption... options) {
        for (var option : options) {
            switch (option) {
                case NOTIFY_ONLY_IF_ISSUER_IS_NOT_US:
                    var consumerOld = consumer;
                    consumer = event -> {
                        if (!event.getIssuer().equals(this.name)) {
                            consumerOld.accept(event);
                        }
                    };
                    break;
                default:
                    LOG.log(
                            Level.WARNING,
                            "Unexpected RegistrationOption variant will be ignored: " + option,
                            new Exception("see this stack trace")
                    );
                    break;
            }
        }
        this.registerEventListener(command, consumer);
    }

    public synchronized void registerEventListener(@Nonnull Event.Command command, @Nonnull Consumer<Event> consumer) {
        this.listener
                .computeIfAbsent(command, c -> new ArrayList<>())
                .add(consumer);
    }

    private void startEventDirWatchService(Path eventDirectory) throws IOException {
        var fs = eventDirectory.getFileSystem();
        var ws = fs.newWatchService();
        eventDirectory.register(ws, ENTRY_MODIFY);


        var thread = new Thread(() -> this.watchForNewEvents(ws));
        thread.setName(getClass().getSimpleName() + ".WatchService");
        thread.setDaemon(true);
        thread.start();
    }

    private void watchForNewEvents(WatchService ws) {
        while (true) {
            try {
                var sleepTimeMillis = getMillisUntilNextLockExpires().orElse(MAX_POLL_TIME_INTERVAL);
                var pollTimeMillis = Math.min(
                        MAX_POLL_TIME_INTERVAL,
                        Math.max(MIN_POLL_TIME_INTERVALL, sleepTimeMillis)
                );

                var key = ws.poll(pollTimeMillis, TimeUnit.MILLISECONDS);
                if (key != null) {
                    if (!key.reset() || !key.isValid()) {
                        key.cancel();
                        continue;
                    }
                    for (var event : key.pollEvents()) {
                        try {
                            var path = (Path) event.context();
                            int id   = Integer.parseInt(path.getFileName().toString());

                            while (id > eventCounter) {
                                if (loadNextEvent().isEmpty()) {
                                    break;
                                }
                            }

                        } catch (RuntimeException | LockException e) {
                            LOG.log(Level.WARNING, "Failed to react on watch event", e);
                        }
                    }
                }

                try {
                    while (true) {
                        var path = nextEventPath();
                        if (Files.exists(path)) {
                            if (loadNextEvent().isEmpty()) {
                                // it did not load successfully
                                LOG.log(Level.SEVERE, "Failed to load event from " + path);
                            }
                        } else {
                            break;
                        }
                    }
                } catch (LockException e) {
                    e.printStackTrace();
                }

                this.checkForExpiredLocks();
                this.deleteOldEventFiles();
            } catch (InterruptedException ignored) {
            } catch (ClosedWatchServiceException e) {
                LOG.log(Level.SEVERE, "Watch service closed", e);
                break;
            }
        }
    }

    private synchronized Optional<Long> getMillisUntilNextLockExpires() {
        var expiresNext = Optional.<Long>empty();
        for (var entry : this.locks.entrySet()) {
            var expiresAt = entry.getValue().getTime() + entry
                    .getValue()
                    .getDuration() + LOCK_DURATION_OFFSET + DURATION_SURELY_OUT_OF_DATE;
            if (expiresAt < System.currentTimeMillis()) {
                return Optional.of(0L);
            } else {
                if (expiresNext.isEmpty() || expiresNext.get() > expiresAt) {
                    expiresNext = Optional.of(expiresAt);
                }
            }
        }
        return expiresNext.map(v -> Math.max(0, v - System.currentTimeMillis()));
    }

    private synchronized void checkForExpiredLocks() {
        var expired = new ArrayList<Event>(this.locks.size());
        for (var entry : this.locks.entrySet()) {
            if (isSurelyOutOfDate(entry.getValue())) {
                LOG.warning("Detected surely expired " + entry.getValue());
                expired.add(entry.getValue());
            }
        }
        expired.forEach(event -> {
            try {
                this.publishEvent(id -> new Event(
                        event.getId(),
                        Event.Command.RELEASE,
                        System.currentTimeMillis(),
                        0,
                        event.getSubject(),
                        this.name
                ));
            } catch (LockException e) {
                LOG.log(Level.WARNING, "Failed to publish command to release expired event", e);
            }
        });
    }

    public boolean release(Token token) {
        try {
            this.publishEvent(randomUuid -> {
                ensureEventForTokenIsValid(token);
                return new Event(
                        token.getId(),
                        Event.Command.RELEASE,
                        System.currentTimeMillis(),
                        0,
                        token.getSubject(),
                        name
                );
            });
            return true;
        } catch (LockException e) {
            e.printStackTrace();
            return isLockedByThisInstance(token.getSubject());
        }
    }

    public Token extend(Token token, long duration) throws LockException {
        return this.publishEvent(randomUuid -> {
            ensureEventForTokenIsValid(token);
            return new Event(
                    token.getId(),
                    Event.Command.EXTEND,
                    System.currentTimeMillis(),
                    duration,
                    token.getSubject(),
                    name
            );
        });
    }

    private void ensureEventForTokenIsValid(Token token) throws LockException {
        var event = this.locks.get(token.getSubject());
        if (event == null) {
            throw new LockException("Lock for " + token + " is unknown");
        } else if (!event.getId().equals(token.getId())) {
            throw new LockException("Lock acquired by some other token=" + event.getId() + ", but yours is=" + token.getId());
        }
    }

    public boolean isLocked(String subject) {
        return getValidLock(subject).isPresent();
    }

    public boolean isLockedByThisInstance(String subject) {
        return getValidLock(subject).map(e -> this.name.equals(e.getIssuer())).orElse(Boolean.FALSE);
    }

    public boolean isLockedByAnotherInstance(String subject) {
        return getValidLock(subject).map(e -> !this.name.equals(e.getIssuer())).orElse(Boolean.FALSE);
    }

    private synchronized Optional<Event> getValidLock(String subject) {
        ensureLocksAreUpToDate();
        var lock = this.locks.get(subject);
        LOG.fine("Lock lookup for the same subject: " + lock);
        if (lock != null && lock.getTime() + lock.getDuration() + LOCK_DURATION_OFFSET >= System.currentTimeMillis()) {
            return Optional.of(lock);
        } else {
            return Optional.empty();
        }
    }

    private void ensureLocksAreUpToDate() {
        var maxEventCounter = getMaxEventCounterNoThrows();
        while (true) {
            try {
                // ensure all events have been loaded
                if (loadNextEvent().isEmpty() && maxEventCounter.map(c -> c < this.eventCounter).orElse(true)) {
                    break;
                }
            } catch (LockException e) {
                e.printStackTrace();
            }
        }
    }

    public Token lock(String subject, long duration) throws LockException {
        return this.publishEvent(id -> {
            this.ensureSubjectLockUnknown(subject);
            return new Event(id, Event.Command.LOCK, System.currentTimeMillis(), duration, subject, name);
        });
    }

    private synchronized void ensureSubjectLockUnknown(String subject) throws LockAlreadyExistsException {
        Event event = this.locks.get(subject);
        if (event != null) {
            var lockedUntil = event.getTime() + event.getDuration() + (event.getDuration() > 0
                                                                       ? LOCK_DURATION_OFFSET
                                                                       : 0);
            if (lockedUntil >= System.currentTimeMillis()) {
                throw new LockAlreadyExistsException(subject, lockedUntil);
            }
        }
    }

    public void publishCommand(@Nonnull Event.Command command, @Nonnull String subject) throws LockException {
        this.publishCommand(command, subject, 0);
    }

    public void publishCommand(
            @Nonnull Event.Command command,
            @Nonnull String subject,
            long duration) throws LockException {
        this.publishEvent(id -> new Event(id, command, System.currentTimeMillis(), duration, subject, this.name));
    }

    private synchronized Token publishEvent(@Nonnull EventSupplier supplier) throws LockException {
        try {
            var path     = this.nextEventPath();
            var pathLock = path.resolveSibling(COMMON_LOCK_FILE);
            var event    = supplier.getCheckedEvent(UUID.randomUUID().toString());
            var token    = new Token(event.getId(), path, event.getSubject(), event.getTime());

            try (var channel = FileChannel.open(pathLock, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                try (var lock = channel.lock()) {
                    if (Files.exists(path)) {
                        throw new IOException("Target file already exists: " + path);
                    }
                    try {
                        Files.write(
                                path,
                                Collections.singleton(new TomlWriter().write(event)),
                                StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE_NEW
                        );
                    } finally {
                        Files.deleteIfExists(pathLock);
                    }
                }
            }

            // always returns a valid event
            var read = this.loadNextEvent();

            if (read.isEmpty()) {
                throw new LockException("Failed to read written event, path: " + path);

            } else if (read.get().equals(event)) {
                // it was me all along!
                return token;

            } else {
                // someone else interfered... well, lets try again
                return this.publishEvent(supplier);
            }
        } catch (IOException e) {
            if (this.loadNextEvent().isPresent()) {
                return this.publishEvent(supplier);
            } else {
                throw new LockException("Internal communication error, failed to lock", e);
            }
        }
    }

    private synchronized void processEvent(@Nonnull Path path, @Nonnull Event event) {
        logEvent(path, event);
        switch (event.getCommand()) {
            case LOCK:
            case EXTEND:
                this.locks.put(event.getSubject(), event);
                LOG.fine("ADD/UPDATE lock for subject " + event.getSubject()
                                 + ", time=" + event.getTime()
                                 + ", duration=" + event.getDuration()
                );
                break;
            case RELEASE:
                var lock = this.locks.get(event.getSubject());
                if (lock != null && Objects.equals(lock.getId(), event.getId())) {
                    this.locks.remove(event.getSubject());
                    LOG.fine("REMOVE lock for subject " + event.getSubject());
                } else {
                    LOG.warning("RELEASE REFUSED for subject " + event.getSubject() + " because " + (lock != null
                                                                                                     ? "id mismatch"
                                                                                                     : "unknown"));
                }
                break;
            case KILL:
                break;
        }

        // TODO performance is crying :(
        // it needs  to be async to this thread and separating each listener
        // from the other might be reasonable...?
        Stream.ofNullable(this.listener.get(event.getCommand()))
              .flatMap(List::stream)
              .forEach(listener -> {
                  var thread = new Thread(() -> listener.accept(event));
                  thread.setName(event.getId() + ".evt");
                  thread.start();
              });
    }

    private void initEventCounter() throws IOException {
        getMinEventCounter()
                .filter(a -> a >= this.eventCounter)
                .ifPresent(next -> {
                    this.eventCounter = next;
                });
    }

    @Nonnull
    private Optional<Integer> getMinEventCounter() throws IOException {
        try (var stream = Files.list(eventDirectory)) {
            return stream
                    .flatMap(this::parseEventCounterFromPath)
                    .min(Comparator.comparingInt(a -> a));
        }
    }

    @Nonnull
    private Optional<Integer> getMaxEventCounter() throws IOException {
        try (var stream = Files.list(eventDirectory)) {
            return stream
                    .flatMap(this::parseEventCounterFromPath)
                    .max(Comparator.comparingInt(a -> a));
        }
    }

    @Nonnull
    private Optional<Integer> getMaxEventCounterNoThrows() {
        try {
            return this.getMaxEventCounter();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to determine the max event counter");
            return Optional.empty();
        }
    }

    private Stream<Integer> parseEventCounterFromPath(Path p) {
        try {
            return Stream.of(Integer.parseInt(p.getFileName().toString()));
        } catch (NumberFormatException ee) {
            return Stream.empty();
        }
    }

    private Path nextEventPath() {
        return this.nextEventPath(eventCounter);
    }

    private Path nextEventPath(int counter) {
        return this.eventDirectory.resolve(this.getEventFileNameForCurrentCounter(counter));
    }

    private String getEventFileNameForCurrentCounter(int counter) {
        return String.format("%08d", counter);
    }

    @Nonnull
    private synchronized Optional<Event> loadNextEvent() throws LockException {

        for (int i = 0; i < LOCK_RETRY_READ_MAX_TRIALS; ++i) {
            Path  path  = null;
            Event event = null;
            try {
                path  = this.nextEventPath();
                event = loadEvent(path);

                if (event.isIncomplete() && !Files.exists(nextEventPath(this.eventCounter + 1))) {
                    if (i > LOCK_RETRY_READ_MAX_TRIALS / 20) {
                        LOG.warning("Event " + path + " still seems incomplete, attempt " + i);
                    }
                    ensureSleepMs(LOCK_RETRY_READ_COOLDOWN_MS);
                    continue;
                } else {
                    event.check();
                }

                this.processEvent(path, event);

                this.eventCounter += 1;
                return Optional.of(event);
            } catch (NoSuchFileException | FileNotFoundException e) {
                LOG.fine("Failed to read next event because there is none");
                return Optional.empty();
            } catch (Throwable e) {
                var cooledDownAtLeastOnceAndHasNext = i > 0 && !fileJustModified(path) && Files.exists(nextEventPath(
                        this.eventCounter + 1));
                if (i > LOCK_RETRY_MIN_THRESHOLD && (i + 1 == LOCK_RETRY_READ_MAX_TRIALS || !fileJustModified(path) || cooledDownAtLeastOnceAndHasNext)) {
                    // max retries exceeded or file probably not actively written to
                    if (path != null && Files.exists(path)) {
                        this.eventCounter += 1; // do not try again
                    }
                    LOG.warning("Aborting read attempt after " + i + " failures, " +
                                        "justModified=" + fileJustModified(path) + ", " +
                                        "hasNext=" + Files.exists(nextEventPath(this.eventCounter + 1))
                    );
                    throw new LockException("Failed to parse event file: " + path, e);
                } else {
                    LOG.warning("Failed to read event, retrying after cooldown");
                    ensureSleepMs(LOCK_RETRY_READ_COOLDOWN_MS);
                }
            }
        }
        return Optional.empty();
    }

    public static void ensureSleepMs(long ms) {
        var start = System.currentTimeMillis();
        while (true) {
            var now = System.currentTimeMillis();
            if (now - start >= ms) {
                break;
            } else {
                try {
                    Thread.sleep(ms - (now - start));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean fileJustModified(@Nullable Path path) {
        return path != null && System.currentTimeMillis() - path.toFile().lastModified() < 30_000;
    }

    private void deleteOldEventFiles() {
        try {
            this.tryDeleteOldEventFiles();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to delete old events", e);
        }
    }

    private void tryDeleteOldEventFiles() throws IOException {
        var list = sortedEventFileList();
        var diff = list.size() - MAX_OLD_EVENT_FILE_COUNT;

        for (int i = 0; i < diff; ++i) {
            Path path = list.get(i);

            if (isEventFile(path)) {
                try {
                    if (isSurelyOutOfDate(loadEvent(path))) {
                        Files.deleteIfExists(path);
                    } else {
                        break;
                    }
                } catch (NoSuchFileException | FileNotFoundException e) {
                    // already gone, can be ignored
                } catch (IOException e) {
                    // probably corrupt file
                    LOG.log(Level.WARNING, "Failed to load file to check for last usage", e);
                    if (path.toFile().lastModified() + DURATION_FOR_UNREADABLE_FILES < System.currentTimeMillis()) {
                        Files.deleteIfExists(path);
                    }
                }
            }
        }
    }

    private boolean isEventFile(Path path) {
        try {
            Integer.parseInt(path.getFileName().toString());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private List<Path> sortedEventFileList() throws IOException {
        try (var files = Files.list(eventDirectory)) {
            return files
                    .sorted(Comparator.comparing(Path::getFileName))
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    private static boolean isSurelyOutOfDate(@Nonnull Event event) {
        return event.getTime() + event.getDuration() + LOCK_DURATION_OFFSET + DURATION_SURELY_OUT_OF_DATE < System.currentTimeMillis();
    }

    private static Event loadEvent(@Nonnull Path path) throws IOException {
        var content = Files.readString(path);
        return new Toml().read(content).to(Event.class);
    }

    private static void logEvent(@Nonnull Path path, @Nonnull Event event) {
        LOG.info(path.getFileName() + " " + event.getIssuer() + ": " + event.getCommand() + " " + event.getSubject());
    }

    interface EventSupplier {
        Event getCheckedEvent(String id) throws LockException;
    }

    public enum RegistrationOption {
        NOTIFY_ONLY_IF_ISSUER_IS_NOT_US
    }
}
