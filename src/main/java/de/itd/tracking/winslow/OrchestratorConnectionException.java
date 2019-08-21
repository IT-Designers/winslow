package de.itd.tracking.winslow;

public class OrchestratorConnectionException extends OrchestratorException {
    public OrchestratorConnectionException(String message) {
        super(message);
    }

    public OrchestratorConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
