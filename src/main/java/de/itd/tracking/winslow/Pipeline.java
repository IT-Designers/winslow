package de.itd.tracking.winslow;

import de.itd.tracking.winslow.config.PipelineDefinition;
import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public interface Pipeline {

    enum PipelineStrategy {
        MoveForwardUntilEnd, MoveForwardOnce,
    }

    enum PauseReason {
        ConfirmationRequired, FurtherInputRequired, StageFailure
    }

    enum ResumeNotification {
        Confirmation
    }

    @Nonnull
    PipelineDefinition getDefinition();

    @Nonnull
    Optional<? extends Stage> getRunningStage();

    /**
     * @return Either the currently running stage or the last completed one
     */
    @Nonnull
    Optional<? extends Stage> getMostRecentStage();

    @Nonnull
    Stream<? extends Stage> getCompletedStages();

    @Nonnull
    Stream<? extends Stage> getAllStages();

    @Nonnull
    Optional<? extends Stage> getStage(@Nonnull String id);

    default void requestPause() {
        requestPause(null);
    }

    void requestPause(@Nullable PauseReason reason);

    boolean isPauseRequested();

    @Nonnull
    Optional<PauseReason> getPauseReason();

    default void resume() {
        resume(null);
    }

    void resume(@Nullable ResumeNotification notification);

    int getNextStageIndex();

    void setNextStageIndex(int index) throws IndexOutOfBoundsException;

    default Optional<StageDefinition> getNextStage() {
        try {
            return Optional.ofNullable(getDefinition().getStageDefinitions().get(getNextStageIndex()));
        } catch (IndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }

    default void setNextStage(StageDefinition definition) throws IndexOutOfBoundsException {
        this.setNextStageIndex(getDefinition().getStageDefinitions().indexOf(definition));
    }

    @Nonnull
    PipelineStrategy getStrategy();

    void setStrategy(PipelineStrategy strategy);

    @Nonnull
    Map<String, String> getEnvironment();
}
