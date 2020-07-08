package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.config.ExecutionGroup;

import javax.annotation.Nonnull;

public class ThereIsStillAnActiveExecutionGroupException extends Throwable {

    private final @Nonnull Pipeline pipeline;
    private final @Nonnull ExecutionGroup activeExecution;

    public ThereIsStillAnActiveExecutionGroupException(@Nonnull Pipeline pipeline, @Nonnull ExecutionGroup activeExecution) {
        super("Within pipeline " + pipeline.getProjectId() + " there is the active execution group " + activeExecution.getId());
        this.pipeline = pipeline;
        this.activeExecution = activeExecution;
    }

    @Nonnull
    public Pipeline getPipeline() {
        return pipeline;
    }

    @Nonnull
    public ExecutionGroup getActiveExecution() {
        return activeExecution;
    }
}
