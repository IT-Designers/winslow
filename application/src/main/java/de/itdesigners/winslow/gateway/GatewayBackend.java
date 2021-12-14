package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.Backend;
import de.itdesigners.winslow.PipelineRepository;
import de.itdesigners.winslow.StageHandle;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.nomad.SubmissionToNomadJobAdapter;
import de.itdesigners.winslow.pipeline.StageId;
import de.itdesigners.winslow.pipeline.Submission;
import de.itdesigners.winslow.pipeline.SubmissionResult;
import de.itdesigners.winslow.project.ProjectRepository;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

public class GatewayBackend implements Backend, Closeable, AutoCloseable {

    private final @Nonnull PipelineRepository pipelines;
    private final @Nonnull ProjectRepository  projects;

    public GatewayBackend(@Nonnull PipelineRepository pipelines, @Nonnull ProjectRepository projects) {
        this.pipelines = pipelines;
        this.projects  = projects;
    }

    @Nonnull
    @Override
    public Stream<String> listStages() throws IOException {
        return Stream.empty();
    }

    @Nonnull
    @Override
    public Optional<State> getState(@Nonnull StageId stageId) throws IOException {
        return Optional.empty();
    }

    @Nonnull
    @Override
    public Optional<State> getState(@Nonnull String pipeline, @Nonnull String stage) throws IOException {
        return Optional.empty();
    }

    @Override
    public void delete(@Nonnull String pipeline, @Nonnull String stage) throws IOException {

    }

    @Override
    public void stop(@Nonnull String stage) throws IOException {

    }

    @Override
    public void kill(@Nonnull String stage) throws IOException {

    }

    @Nonnull
    @Override
    public SubmissionResult submit(@Nonnull Submission submission) throws IOException {
        return new SubmissionResult(
                SubmissionToNomadJobAdapter.createStage(submission),
                spawnStageHandle(submission.getStageDefinition(), submission.getId())
        );
    }

    @Nonnull
    private StageHandle spawnStageHandle(@Nonnull StageDefinition stageDefinition, @Nonnull StageId stageId) throws IOException {
        switch (stageDefinition.getType()) {
            case AndGateway:
                return new GatewayStageHandle(new AndGateway(pipelines, projects, stageDefinition, stageId));
            case XOrGateway:
                return new GatewayStageHandle(new XOrGateway(pipelines, stageDefinition, stageId));

            case Execution:
                break;
        }
        throw new IOException("Invalid StageType " + stageDefinition.getType());
    }

    @Override
    public boolean isCapableOfExecuting(@Nonnull StageDefinition stage) {
        return stage.getType().isGateway();
    }

    @Override
    public void close() throws IOException {

    }
}
