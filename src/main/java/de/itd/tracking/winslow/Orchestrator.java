package de.itd.tracking.winslow;

import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;
import de.itd.tracking.winslow.project.Project;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface Orchestrator {


    @Nonnull
    Optional<RunningStage> getCurrentlyRunningStage(@Nonnull Project project);

    @Nonnull
    Optional<RunningStage> startNextStage(@Nonnull Project project, @Nonnull Environment environment);

    @Nonnull
    PreparedStage prepare(Pipeline pipeline, Stage stage, Environment environment) throws OrchestratorException;


    @Nonnull
    default Optional<PreparedStage> prepareNoThrows(Pipeline pipeline, Stage stage, Environment environment) {
        try {
            return Optional.of(prepare(pipeline, stage, environment));
        } catch (OrchestratorException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
