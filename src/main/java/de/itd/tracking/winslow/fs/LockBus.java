package de.itd.tracking.winslow.fs;

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

    public static final int LOCK_DURATION_OFFSET          = 1_000;
    public static final int DURATION_SURELY_OUT_OF_DATE   = 5_000;
    public static final int DURATION_FOR_UNREADABLE_FILES = 25_000;
    public static final int MAX_OLD_EVENT_FILE_COUNT      = 25;

    private final String             name;
    private final Path               eventDirectory;
    private final Map<String, Event> locks = new HashMap<>();

    private final Map<Event.Command, List<Consumer<Event>>> listener = new EnumMap<>(Event.Command.class);

    private int eventCounter = 0;

    public LockBus(String name, Path eventDirectory) throws IOException, LockException {
        this.name           = name;
        this.eventDirectory = eventDirectory;
        if (!eventDirectory.toFile().exists() && !eventDirectory.toFile().mkdirs()) {
            throw new IOException("Failed to create event directory at: " + eventDirectory);
        }
        if (eventDirectory.toFile().exists() && !eventDirectory.toFile().isDirectory()) {
            throw new IOException("Path to event directory is not a directory: " + eventDirectory);
        }

        this.ensureLocksAreUpToDate();
        this.startEventDirWatchService(eventDirectory);
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
                var sleepTimeMillis = getMillisUntilNextLockExpires().orElse(60 * 1_000L);
                var key             = ws.poll(Math.min(1_000, Math.max(10, sleepTimeMillis)), TimeUnit.MILLISECONDS);
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
                                if (loadNextEvent(0).isEmpty()) {
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
                            loadNextEvent(5);
                        } else {
                            break;
                        }
                    }
                } catch (LockException | IOException e) {
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
            return false;
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

    public synchronized boolean isLocked(String subject) {
        ensureLocksAreUpToDate();
        var lock = this.locks.get(subject);
        LOG.fine("Lock lookup for the same subject: " + lock);
        return lock != null && lock.getTime() + lock.getDuration() + LOCK_DURATION_OFFSET >= System.currentTimeMillis();
    }

    private void ensureLocksAreUpToDate() {
        while (true) {
            try {
                // ensure all events have been loaded
                if (loadNextEvent().isEmpty()) {
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
        this.publishEvent(id -> new Event(id, command, System.currentTimeMillis(), 0, subject, this.name));
    }

    private synchronized Token publishEvent(@Nonnull EventSupplier supplier) throws LockException {
        try {
            var path     = this.nextEventPath();
            var pathLock = path.resolveSibling(".lock"); // TODO
            var event    = supplier.getCheckedEvent(UUID.randomUUID().toString());
            var token    = new Token(event.getId(), path, event.getSubject(), event.getTime());

            try (var channel = FileChannel.open(pathLock, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                try (var lock = channel.lock()) {
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
                throw new LockException("Failed to read written event");

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

    private synchronized void processEvent(Event event) {
        logEvent(event);
        switch (event.getCommand()) {
            case LOCK:
            case EXTEND:
                this.locks.put(event.getSubject(), event);
                LOG.fine("ADD/UPDATE lock for subject " + event.getSubject());
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
              .forEach(listener -> new Thread(() -> listener.accept(event)).start());
    }

    private Path nextEventPath() throws IOException {
        try (var stream = Files.list(eventDirectory)) {
            stream
                    .flatMap(p -> {
                        try {
                            return Stream.of(Integer.parseInt(p.getFileName().toString()));
                        } catch (NumberFormatException ee) {
                            return Stream.empty();
                        }
                    })
                    .filter(a -> a >= this.eventCounter)
                    .min(Comparator.comparingInt(a -> a))
                    .ifPresent(next -> this.eventCounter = next);
            return this.eventDirectory.resolve(this.getEventFileNameForCurrentCounter());
        }
    }

    private String getEventFileNameForCurrentCounter() {
        return String.format("%08d", eventCounter);
    }

    @Nonnull
    private Optional<Event> loadNextEvent() throws LockException {
        return this.loadNextEvent(10);
    }

    @Nonnull
    private synchronized Optional<Event> loadNextEvent(int retryCount) throws LockException {
        for (int i = 0; i < retryCount; ++i) {
            Path  path  = null;
            Event event = null;
            try {
                path  = this.nextEventPath();
                event = loadEvent(path);
                event.check();

                this.processEvent(event);

                this.eventCounter += 1;
                return Optional.of(event);
            } catch (NoSuchFileException | FileNotFoundException e) {
                LOG.fine("Failed to read next event because there is none");
                return Optional.empty();
            } catch (Throwable e) {
                if (i + 1 == retryCount || !fileJustCreated(path)) {
                    // max retries exceeded or file probably not actively written to
                    if (path != null && Files.exists(path)) {
                        this.eventCounter += 1; // do not try again
                    }
                    throw new LockException("Failed to parse event file: " + path, e);
                } else {
                    ensureSleepMs(100);
                }
            }
        }
        return Optional.empty();
    }

    private static void ensureSleepMs(long ms) {
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

    private static boolean fileJustCreated(@Nullable Path path) {
        return path != null && path.toFile().lastModified() - System.currentTimeMillis() < 1000;
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
            Path  path  = list.get(i);
            Event event = null;

            try {
                event = loadEvent(path);
            } catch (IOException e) {
                // probably corrupt file
                LOG.log(Level.WARNING, "Failed to load file to check for last usage", e);
                if (path.toFile().lastModified() + DURATION_FOR_UNREADABLE_FILES < System.currentTimeMillis()) {
                    Files.deleteIfExists(path);
                }
                continue;
            }

            if (isSurelyOutOfDate(event)) {
                Files.deleteIfExists(path);
            } else {
                break;
            }
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

    private static void logEvent(Event event) {
        LOG.info(event.getIssuer() + ": " + event.getCommand() + " " + event.getSubject());
    }

    interface EventSupplier {
        Event getCheckedEvent(String id) throws LockException;
    }
}
