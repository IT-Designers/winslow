package de.itd.tracking.winslow.fs;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import org.springframework.security.authentication.LockedException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class LockBus {

    private static final Logger LOG = Logger.getLogger(LockBus.class.getSimpleName());

    public static final int LOCK_DURATION_OFFSET = 1_000;

    private final Path               eventDirectory;
    private final Map<String, Event> locks = new HashMap<>();

    private int eventCounter = 0;

    public LockBus(Path eventDirectory) throws IOException {
        this.eventDirectory = eventDirectory;
        if (!eventDirectory.toFile().exists() && !eventDirectory.toFile().mkdirs()) {
            throw new IOException("Failed to create event directory at: " + eventDirectory);
        }
        if (eventDirectory.toFile().exists() && !eventDirectory.toFile().isDirectory()) {
            throw new IOException("Path to event directory is not a directory: " + eventDirectory);
        }

        try {
            Files.list(eventDirectory).sorted().findFirst().ifPresent(file -> {
                this.eventCounter = Integer.parseInt(file.getFileName().toString());
            });
        } catch (NumberFormatException e) {
            throw new IOException("Invalid name of event file", e);
        }
    }

    public boolean release(Token token) {
        try {
            this.publishEvent(randomUuid -> {
                ensureEventForTokenIsValid(token);
                return new Event(token.getId(), Event.Command.RELEASE, System.currentTimeMillis(), 0, token.getSubject());
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
            return new Event(token.getId(), Event.Command.EXTEND, System.currentTimeMillis(), duration, token.getSubject());
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

    private synchronized Token publishEvent(EventSupplier supplier) throws LockException {
        var path  = this.nextEventPath();
        var event = supplier.getCheckedEvent(UUID.randomUUID().toString());
        var token = new Token(event.getId(), path, event.getSubject(), event.getTime());

        System.out.println(new TomlWriter().write(event));

        try {
            Files.write(path, Collections.singleton(new TomlWriter().write(event)), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            this.processEvent(event);
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

    private Path nextEventPath() {
        return this.eventDirectory.resolve(this.nextEventFilename());
    }

    private String nextEventFilename() {
        return String.format("%08d", eventCounter);
    }

    private boolean loadNextEvent() throws LockException {
        return this.loadNextEvent(10);
    }

    private synchronized boolean loadNextEvent(int retryCount) throws LockException {
        var path = this.nextEventPath();
        try {
            var content = Files.readString(path);
            var event   = new Toml().read(content).to(Event.class);

            if (event.getTime() + event.getDuration() + 10 * LOCK_DURATION_OFFSET < System.currentTimeMillis()) {
                if (Files.list(path.getParent()).count() > 10) {
                    if (!path.toFile().delete()) {
                        LOG.warning("Failed to delete old event file: " + event);
                    }
                }
            } else {
                this.processEvent(event);
            }

            this.eventCounter += 1;
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            if (retryCount > 0 && path.toFile().lastModified() - System.currentTimeMillis() < 1000) {
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
