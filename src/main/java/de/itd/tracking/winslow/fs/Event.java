package de.itd.tracking.winslow.fs;

public class Event {

    private final String  id;
    private final Command command;
    private final long    time;
    private final long    duration;
    private final String  subject;
    private final String  issuer;

    public Event(String id, Command command, long time, long duration, String subject, String issuer) {
        this.id       = id;
        this.command  = command;
        this.time     = time;
        this.duration = duration;
        this.subject  = subject;
        this.issuer   = issuer;
    }

    public String getId() {
        return id;
    }

    public Command getCommand() {
        return command;
    }

    public long getTime() {
        return time;
    }

    public long getDuration() {
        return duration;
    }

    public String getSubject() {
        return subject;
    }

    public String getIssuer() {
        return issuer;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{id=" + id + ",command=" + command + ",time=" + time + ",duration=" + duration + ",subject='" + subject + "'}#" + hashCode();
    }

    public enum Command {
        LOCK, RELEASE, EXTEND, KILL
    }
}
