package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.Backend;
import de.itdesigners.winslow.PipelineRepository;
import de.itdesigners.winslow.StageHandle;
import de.itdesigners.winslow.config.StageAndGatewayDefinition;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.config.StageXOrGatwayDefinition;
import de.itdesigners.winslow.pipeline.StageId;
import de.itdesigners.winslow.pipeline.Submission;
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

    @Nonnull
    @Override
    public StageHandle submit(@Nonnull Submission submission) throws IOException {
        return spawnStageHandle(
                submission
                        .getExtension(GatewayExtension.class)
                        .orElseThrow(() -> new IOException("Missing GatewayExtension"))
                        .stageDefinition(),
                submission.getId()
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
        } else if (stageDefinition instanceof StageXOrGatwayDefinition stageXOrGatwayDefinition) {
            return new GatewayStageHandle(new XOrGateway(pipelines, projects, stageXOrGatwayDefinition, stageId));
        } else {
            throw new IOException("Invalid StageType " + stageDefinition.getClass().toString());
        }
    }


    @Override
    public void close() throws IOException {

    }
}
