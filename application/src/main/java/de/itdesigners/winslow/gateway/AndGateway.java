package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.PipelineRepository;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.Stage;
import de.itdesigners.winslow.pipeline.StageId;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class AndGateway extends Gateway {

    private final @Nonnull PipelineRepository pipelines;
    private final @Nonnull StageDefinition    stageDefinition;
    private                StageId            stageId;

    public AndGateway(
            @Nonnull PipelineRepository pipelines,
            @Nonnull StageDefinition stageDefinition,
            @Nonnull StageId stageId) {
        this.pipelines = pipelines;
        this.stageDefinition = stageDefinition;
        this.stageId = stageId;
    }

    @Override
    public void execute() {
        this.log(Level.INFO, "AndGateway!");
        // here Samuel, have fun
    }
}
