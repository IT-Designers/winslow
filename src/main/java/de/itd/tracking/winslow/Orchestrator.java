package de.itd.tracking.winslow;

import de.itd.tracking.winslow.project.Project;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public interface Orchestrator {

    @Nonnull
    Pipeline createPipeline(@Nonnull Project project) throws OrchestratorException;

    @Nonnull
    Optional<? extends Pipeline> getPipeline(@Nonnull Project project) throws OrchestratorException;

    default Optional<? extends Pipeline> getPipelineOmitExceptions(@Nonnull Project project) {
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

    @Nonnull
    Stream<LogEntry> getLogs(@Nonnull Project project, @Nonnull String stageId);

    @Nonnull
    Optional<Integer> getProgressHint(@Nonnull Project project);

}
