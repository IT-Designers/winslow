package de.itdesigners.winslow.api.project;

import javax.annotation.Nullable;

public class UpdatePauseRequest {
    public           boolean paused;
    public @Nullable String  strategy;

    public UpdatePauseRequest(boolean paused, @Nullable String strategy) {
        this.paused   = paused;
        this.strategy = strategy;
    }
}
