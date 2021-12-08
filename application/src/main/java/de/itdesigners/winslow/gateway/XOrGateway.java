package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.PipelineRepository;
import de.itdesigners.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class XOrGateway extends Gateway {

    private final @Nonnull PipelineRepository pipelines;
    private final @Nonnull StageDefinition    stageDefinition;

    public XOrGateway(@Nonnull PipelineRepository pipelines, @Nonnull StageDefinition stageDefinition) {
        this.pipelines       = pipelines;
        this.stageDefinition = stageDefinition;
    }

    @Override
    public void execute() {
        this.log(Level.INFO, "XOrGateway!");
        // here Samuel, have fun
    }
}
