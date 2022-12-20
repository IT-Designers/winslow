package de.itdesigners.winslow.api.pipeline;

public enum State {
    RUNNING,
    PAUSED,
    SUCCEEDED,
    FAILED,
    PREPARING,
    ENQUEUED,
    SKIPPED
}
