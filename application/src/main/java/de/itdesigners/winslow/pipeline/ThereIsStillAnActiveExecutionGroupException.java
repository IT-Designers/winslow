package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.config.ExecutionGroup;

import javax.annotation.Nonnull;

public class ThereIsStillAnActiveExecutionGroupException extends Throwable {

    private final @Nonnull Pipeline       pipeline;
    private final @Nonnull ExecutionGroup activeExecution;

    public ThereIsStillAnActiveExecutionGroupException(
            @Nonnull Pipeline pipeline,
            @Nonnull ExecutionGroup activeExecution) {
        super("There is the already an active execution group " + activeExecution.getFullyQualifiedId());
        this.pipeline        = pipeline;
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
