package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.PipelineRepository;
import de.itdesigners.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class AndGateway extends Gateway {

    private final @Nonnull PipelineRepository pipelines;
    private final @Nonnull StageDefinition    stageDefinition;

    public AndGateway(@Nonnull PipelineRepository pipelines, @Nonnull StageDefinition stageDefinition) {
        this.pipelines       = pipelines;
        this.stageDefinition = stageDefinition;
    }

    @Override
    public void execute() {
        this.log(Level.INFO, "AndGateway!");
        // here Samuel, have fun
    }
}
