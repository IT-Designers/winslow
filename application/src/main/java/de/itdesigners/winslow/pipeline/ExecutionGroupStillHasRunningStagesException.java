package de.itdesigners.winslow.pipeline;

import de.itdesigners.winslow.config.ExecutionGroup;

import javax.annotation.Nonnull;

public class ExecutionGroupStillHasRunningStagesException extends Exception {

    private final @Nonnull Pipeline       pipeline;
    private final @Nonnull ExecutionGroup activeExecution;

    public ExecutionGroupStillHasRunningStagesException(@Nonnull Pipeline pipeline, @Nonnull ExecutionGroup activeExecution) {
        super("The active execution group of pipeline " + pipeline.getProjectId() + " still has "
                      + activeExecution.getRunningStages().count() + " stage running");
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
