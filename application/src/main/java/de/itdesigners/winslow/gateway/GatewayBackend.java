package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.Backend;
import de.itdesigners.winslow.PipelineRepository;
import de.itdesigners.winslow.StageHandle;
import de.itdesigners.winslow.config.StageDefinition;
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
        var info = submission
                .getExtension(GatewayExtension.class)
                .orElseThrow(() -> new RuntimeException("Missing GatewayExtension"));

        return switch (info.stageDefinition().getType()) {
            case AndGateway -> new GatewayStageHandle(new AndGateway(
                    pipelines,
                    projects,
                    info.stageDefinition(),
                    submission.getId()
            ));
            case XOrGateway -> new GatewayStageHandle(new XOrGateway(
                    pipelines,
                    projects,
                    info.stageDefinition(),
                    submission.getId()
            ));
            case Execution -> throw new IOException("Invalid StageType " + info.stageDefinition().getType());
        };
    }

    @Override
    public boolean isCapableOfExecuting(@Nonnull StageDefinition stage) {
        return stage.getType().isGateway();
    }

    @Override
    public void close() throws IOException {

    }
}
