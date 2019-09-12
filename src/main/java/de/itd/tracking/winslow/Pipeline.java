package de.itd.tracking.winslow;

import de.itd.tracking.winslow.config.PipelineDefinition;
import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public interface Pipeline {

    enum State {
        Running, Paused, AwaitingUserInput
    }

    enum PipelineStrategy {
        MoveForwardUntilEnd, MoveForwardOnce,
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

    void requestPause();

    boolean isPauseRequested();

    void resume();

    int getNextStageIndex();

    void setNextStageIndex(int index) throws IndexOutOfBoundsException;

    default Optional<StageDefinition> getNextStage() {
        try {
            return Optional.of(getDefinition().getStageDefinitions().get(getNextStageIndex()));
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
}
