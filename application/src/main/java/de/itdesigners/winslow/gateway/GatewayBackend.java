package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.Backend;
import de.itdesigners.winslow.PipelineRepository;
import de.itdesigners.winslow.StageHandle;
import de.itdesigners.winslow.config.StageAndGatewayDefinition;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.config.StageXOrGatwayDefinition;
import de.itdesigners.winslow.pipeline.StageId;
import de.itdesigners.winslow.pipeline.Submission;
import de.itdesigners.winslow.pipeline.SubmissionResult;
import de.itdesigners.winslow.project.ProjectRepository;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;

public class GatewayBackend implements Backend, Closeable, AutoCloseable {

    private final @Nonnull PipelineRepository pipelines;
    private final @Nonnull ProjectRepository  projects;

    public GatewayBackend(@Nonnull PipelineRepository pipelines, @Nonnull ProjectRepository projects) {
        this.pipelines = pipelines;
        this.projects  = projects;
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
                submission.createStage(),
                spawnStageHandle(submission.getStageDefinition(), submission.getId())
        );
    }

    @Override
    public boolean isCapableOfExecuting(@Nonnull StageDefinition stage) {
        return stage instanceof StageAndGatewayDefinition || stage instanceof StageXOrGatwayDefinition;
    }

    @Nonnull
    private StageHandle spawnStageHandle(
            @Nonnull StageDefinition stageDefinition,
            @Nonnull StageId stageId) throws IOException {

        if (stageDefinition instanceof StageAndGatewayDefinition stageAndGatewayDefinition) {
            return new GatewayStageHandle(new AndGateway(pipelines, projects, stageAndGatewayDefinition, stageId));
        }

        if (stageDefinition instanceof StageXOrGatwayDefinition stageXOrGatwayDefinition) {
            return new GatewayStageHandle(new XOrGateway(pipelines, projects, stageXOrGatwayDefinition, stageId));
        }

        throw new IOException("Invalid StageType " + stageDefinition.getClass().toString());
    }


    @Override
    public void close() throws IOException {

    }
}
