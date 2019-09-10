package de.itd.tracking.winslow.nomad;

import de.itd.tracking.winslow.Pipeline;
import de.itd.tracking.winslow.config.PipelineDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class NomadPipeline implements Pipeline {

    @Nonnull private final String             projectId;
    @Nonnull private final PipelineDefinition pipelineDefinition;
    @Nonnull private final List<NomadStage>   stages = new ArrayList<>();

    private           boolean          pauseRequested = false;
    private           int              nextStage      = 0;
    @Nonnull private  PipelineStrategy strategy;
    @Nullable private NomadStage       stage;

    public NomadPipeline(@Nonnull String projectId, @Nonnull PipelineDefinition pipelineDefinition) {
        this.projectId          = projectId;
        this.pipelineDefinition = pipelineDefinition;
        this.strategy           = PipelineStrategy.MoveForwardUntilEnd;
    }

    @Nonnull
    public String getProjectId() {
        return projectId;
    }

    @Nonnull
    @Override
    public PipelineDefinition getDefinition() {
        return pipelineDefinition;
    }

    public void pushStage(@Nullable NomadStage stage) {
        if (this.stage != null) {
            this.stages.add(stage);
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
    public Stream<NomadStage> getCompletedStages() {
        return stages.stream();
    }

    @Override
    public void requestPause() {
        this.pauseRequested = true;
    }

    @Override
    public boolean isPauseRequested() {
        return this.pauseRequested;
    }

    @Override
    public void resume() {
        this.pauseRequested = false;
    }

    @Override
    public int getNextStageIndex() {
        return nextStage;
    }

    @Override
    public void setNextStageIndex(int index) throws IndexOutOfBoundsException {
        if (index < 0 || index >= pipelineDefinition.getStageDefinitions().size()) {
            throw new IndexOutOfBoundsException(index);
        } else {
            this.nextStage = index;
        }
    }

    public void incrementNextStageIndex() {
        // index == size indicates there is no further stage to execute
        if (nextStage < pipelineDefinition.getStageDefinitions().size()) {
            nextStage += 1;
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
}
