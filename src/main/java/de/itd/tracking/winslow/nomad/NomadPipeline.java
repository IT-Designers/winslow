package de.itd.tracking.winslow.nomad;

import de.itd.tracking.winslow.Pipeline;
import de.itd.tracking.winslow.config.PipelineDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

public class NomadPipeline implements Pipeline {

    @Nonnull private final String             projectId;
    @Nonnull private final PipelineDefinition definition;
    @Nonnull private final List<NomadStage>   stages = new ArrayList<>();


    private           boolean             pauseRequested     = false;
    @Nullable private PauseReason         pauseReason        = null;
    @Nullable private ResumeNotification  resumeNotification = null;
    @Nullable private Map<String, String> env                = new HashMap<>();

    private           int              nextStageIndex = 0;
    @Nonnull private  PipelineStrategy strategy;
    @Nullable private NomadStage       stage;

    public NomadPipeline(@Nonnull String projectId, @Nonnull PipelineDefinition definition) {
        this.projectId  = projectId;
        this.definition = definition;
        this.strategy   = PipelineStrategy.MoveForwardUntilEnd;
    }

    @Nonnull
    public String getProjectId() {
        return projectId;
    }

    @Nonnull
    @Override
    public PipelineDefinition getDefinition() {
        return definition;
    }

    public void pushStage(@Nullable NomadStage stage) {
        if (this.stage != null) {
            this.stages.add(this.stage);
        }
        this.stage = stage;
    }

    @Nonnull
    @Override
    public Optional<NomadStage> getRunningStage() {
        return Optional.ofNullable(this.stage);
    }

    @Nonnull
    @Override
    public Optional<NomadStage> getMostRecentStage() {
        return getRunningStage().or(() -> {
            if (stages.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(stages.get(stages.size() - 1));
            }
        });
    }

    public int getStageCount() {
        return this.stages.size() + (getRunningStage().isPresent() ? 1 : 0);
    }


    @Nonnull
    @Override
    public Stream<NomadStage> getAllStages() {
        return Stream.concat(getCompletedStages(), getRunningStage().stream());
    }

    @Nonnull
    @Override
    public Optional<NomadStage> getStage(@Nonnull String id) {
        return getAllStages().filter(s -> s.getId().equals(id)).findFirst();
    }

    @Nonnull
    @Override
    public Stream<NomadStage> getCompletedStages() {
        return stages.stream();
    }

    @Override
    public void requestPause(@Nullable PauseReason reason) {
        this.pauseReason    = reason;
        this.pauseRequested = true;
    }

    public void clearPauseReason() {
        this.pauseReason = null;
    }

    @Override
    public boolean isPauseRequested() {
        return this.pauseRequested;
    }

    @Nonnull
    @Override
    public Optional<PauseReason> getPauseReason() {
        return Optional.ofNullable(this.pauseReason);
    }

    @Override
    public void resume(@Nullable ResumeNotification notification) {
        this.pauseRequested     = false;
        this.resumeNotification = notification;
    }

    @Nonnull
    public Optional<ResumeNotification> getResumeNotification() {
        return Optional.ofNullable(resumeNotification);
    }

    public void resetResumeNotification() {
        this.resumeNotification = null;
    }

    @Override
    public int getNextStageIndex() {
        return nextStageIndex;
    }

    @Override
    public void setNextStageIndex(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= definition.getStageDefinitions().size()) {
            throw new IndexOutOfBoundsException(index);
        } else {
            this.nextStageIndex = index;
        }
    }

    public void incrementNextStageIndex() {
        // index == size indicates there is no further stage to execute
        if (nextStageIndex < definition.getStageDefinitions().size()) {
            nextStageIndex += 1;
        }
    }

    @Override
    @Nonnull
    public PipelineStrategy getStrategy() {
        return this.strategy;
    }

    @Override
    public void setStrategy(@Nonnull PipelineStrategy strategy) {
        this.strategy = strategy;
    }

    @Nonnull
    @Override
    public Map<String, String> getEnvironment() {
        if (this.env == null) {
            this.env = new HashMap<>();
        }
        return this.env;
    }
}
