package de.itdesigners.winslow;

public class OrchestratorException extends Exception {
    public OrchestratorException(String message) {
        super(message);
    }

    public OrchestratorException(String message, Throwable cause) {
        super(message, cause);
    }
}
