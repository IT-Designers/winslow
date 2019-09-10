package de.itd.tracking.winslow;

import de.itd.tracking.winslow.config.PipelineDefinition;
import de.itd.tracking.winslow.project.Project;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Function;

public interface Orchestrator {

    @Nonnull
    Pipeline createPipeline(@Nonnull Project project, @Nonnull PipelineDefinition pipelineDefinition) throws OrchestratorException;

    @Nonnull
    Optional<Pipeline> getPipeline(@Nonnull Project project) throws OrchestratorException;

    default Optional<Pipeline> getPipelineOmitExceptions(@Nonnull Project project) {
        try {
            return getPipeline(project);
        } catch (OrchestratorException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Nonnull
    <T> Optional<T> updatePipeline(@Nonnull Project project, @Nonnull Function<Pipeline, T> updater) throws OrchestratorException;

    default <T> Optional<T> updatePipelineOmitExceptions(@Nonnull Project project, @Nonnull Function<Pipeline, T> updater) {
        try {
            return updatePipeline(project, updater);
        } catch (OrchestratorException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

}
