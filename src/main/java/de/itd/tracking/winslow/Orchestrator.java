package de.itd.tracking.winslow;

import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface Orchestrator {

    @Nonnull
    RunningStage start(Pipeline pipeline, Stage stage, Environment environment) throws OrchestratorException;


    @Nonnull
    default Optional<RunningStage> startOrNone(Pipeline pipeline, Stage stage, Environment environment) {
        try {
            return Optional.of(start(pipeline, stage, environment));
        } catch (OrchestratorException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
