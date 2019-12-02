package de.itdesigners.winslow.fs;

public class LockAlreadyExistsException extends LockException {

    private final String subject;
    private final long   until;

    public LockAlreadyExistsException(String subject, long until) {
        super("A lock for the subject '" + subject + "' does already exist");
        this.subject = subject;
        this.until   = until;
    }

    public String getSubject() {
        return subject;
    }

    public long getUntil() {
        return until;
    }
}
