package de.itdesigners.winslow.fs;

import java.nio.file.Path;

public class Token {

    private final String id;
    private final Path   eventOrigin;
    private final String subject;
    private final long   time;

    public Token(String id, Path eventOrigin, String subject, long time) {
        this.id          = id;
        this.eventOrigin = eventOrigin;
        this.subject     = subject;
        this.time        = time;
    }

    public String getId() {
        return id;
    }

    protected Path getEventOrigin() {
        return eventOrigin;
    }

    public String getSubject() {
        return subject;
    }

    public long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{id=" + id + ",origin='" + eventOrigin + "',subject='" + subject + "',time=" + time + "}#" + hashCode();
    }
}
