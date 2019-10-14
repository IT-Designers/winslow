package de.itd.tracking.winslow.fs;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import org.springframework.security.authentication.LockedException;

import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
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

    private final Path               eventDirectory;
    private final Map<String, Event> locks = new HashMap<>();

    private int eventCounter = 0;

    public LockBus(Path eventDirectory) throws IOException, LockException {
        this.eventDirectory = eventDirectory;
        if (!eventDirectory.toFile().exists() && !eventDirectory.toFile().mkdirs()) {
            throw new IOException("Failed to create event directory at: " + eventDirectory);
        }
        if (eventDirectory.toFile().exists() && !eventDirectory.toFile().isDirectory()) {
            throw new IOException("Path to event directory is not a directory: " + eventDirectory);
        }

        this.loadNextEvent();
        this.startEventDirWatchService(eventDirectory);
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
                var key = ws.poll(1, TimeUnit.MINUTES);
                if (key != null) {
                    key.reset();
                    for (var event : key.pollEvents()) {
                        try {
                            var path = (Path) event.context();
                            int id   = Integer.parseInt(path.getFileName().toString());

                            while (id > eventCounter) {
                                if (!loadNextEvent(0)) {
                                    break;
                                }
                            }

                        } catch (RuntimeException | LockException e) {
                            LOG.log(Level.WARNING, "Failed to react on watch event", e);
                        }
                    }
                }
                try {
                    this.deleteOldLocks();
                    this.tryDeleteOldFiles();
                } catch (IOException e) {
                    LOG.log(Level.WARNING, "Failed to delete old events", e);
                }
            } catch (InterruptedException ignored) {
            } catch (ClosedWatchServiceException e) {
                LOG.log(Level.SEVERE, "Watch service closed", e);
                break;
            }
        }
    }

    private synchronized void deleteOldLocks() {
        var toDelete = this.locks
                .values()
                .stream()
                .filter(e -> e.getTime() + e.getDuration() + LOCK_DURATION_OFFSET < System.currentTimeMillis())
                .map(Event::getSubject)
                .collect(Collectors.toUnmodifiableList());
        toDelete.forEach(this.locks::remove);
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
                        token.getSubject()
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
                    token.getSubject()
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
        var lock = this.locks.get(subject);
        return lock != null && lock.getTime() + lock.getDuration() + LOCK_DURATION_OFFSET >= System.currentTimeMillis();
    }

    public Token lock(String subject, long duration) throws LockException {
        return this.publishEvent(id -> {
            this.ensureSubjectLockUnknown(subject);
            return new Event(id, Event.Command.LOCK, System.currentTimeMillis(), duration, subject);
        });
    }

    private synchronized void ensureSubjectLockUnknown(String subject) throws LockAlreadyExistsException {
        Event event = this.locks.get(subject);
        if (event != null) {
            var lockedUntil = event.getTime() + event.getDuration() + LOCK_DURATION_OFFSET;
            if (lockedUntil >= System.currentTimeMillis()) {
                throw new LockAlreadyExistsException(subject, lockedUntil);
            }
        }
    }

    private synchronized Token publishEvent(@Nonnull EventSupplier supplier) throws LockException {
        try {
            var path  = this.nextEventPath();
            var event = supplier.getCheckedEvent(UUID.randomUUID().toString());
            var token = new Token(event.getId(), path, event.getSubject(), event.getTime());

            System.out.println(new TomlWriter().write(event));

            Files.write(
                    path,
                    Collections.singleton(new TomlWriter().write(event)),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW
            );
            try {
                this.processEvent(event);
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Failed to process event, it will be ignored, source: " + path, t);
            }
            this.eventCounter += 1;
            return token;
        } catch (IOException e) {
            if (this.loadNextEvent()) {
                return this.publishEvent(supplier);
            } else {
                throw new LockedException("Internal communication error, failed to lock", e);
            }
        }
    }

    private Path nextEventPath() throws IOException {
        Files.list(eventDirectory).flatMap(p -> {
            try {
                return Stream.of(Integer.parseInt(p.getFileName().toString()));
            } catch (NumberFormatException ee) {
                return Stream.empty();
            }
        }).filter(a -> a >= this.eventCounter).min(Comparator.comparingInt(a -> a)).ifPresent(next -> {
            this.eventCounter = next;
        });
        return this.eventDirectory.resolve(this.getEventFileNameForCurrentCounter());
    }

    private String getEventFileNameForCurrentCounter() {
        return String.format("%08d", eventCounter);
    }

    private boolean loadNextEvent() throws LockException {
        return this.loadNextEvent(10);
    }

    private synchronized boolean loadNextEvent(int retryCount) throws LockException {
        Path path = null;
        try {
            path = this.nextEventPath();

            this.processEvent(loadEvent(path));

            this.eventCounter += 1;
            return true;
        } catch (NoSuchFileException | FileNotFoundException e) {
            LOG.log(Level.WARNING, "Failed to read event");
            return false;
        } catch (IOException e) {
            if (retryCount > 0 && path != null && path.toFile().lastModified() - System.currentTimeMillis() < 1000) {
                e.printStackTrace();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                return this.loadNextEvent(retryCount - 1);
            } else {
                throw new LockException("Failed to parse event file", e);
            }
        }
    }

    private void tryDeleteOldFiles() throws IOException {
        var list = Files
                .list(eventDirectory)
                .sorted(Comparator.comparing(Path::getFileName))
                .collect(Collectors.toUnmodifiableList());
        var diff = list.size() - 10;

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

    private boolean isSurelyOutOfDate(@Nonnull Event event) {
        return event.getTime() + event.getDuration() + LOCK_DURATION_OFFSET + DURATION_SURELY_OUT_OF_DATE < System.currentTimeMillis();
    }

    private Event loadEvent(@Nonnull Path path) throws IOException {
        var content = Files.readString(path);
        var event   = new Toml().read(content).to(Event.class);
        return event;
    }

    private synchronized void processEvent(Event event) {
        switch (event.getCommand()) {
            case LOCK:
            case EXTEND:
                this.locks.put(event.getSubject(), event);
                LOG.fine("ADD/UPDATE lock for subject " + event.getSubject());
                break;
            case RELEASE:
                this.locks.remove(event.getSubject());
                LOG.fine("REMOVE lock for subject " + event.getSubject());
                break;
        }
    }

    interface EventSupplier {
        Event getCheckedEvent(String id) throws LockException;
    }
}
