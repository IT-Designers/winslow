package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.StageAndGatewayDefinitionInfo;
import de.itdesigners.winslow.api.pipeline.StageDefinitionInfo;
import de.itdesigners.winslow.api.pipeline.StageWorkerDefinitionInfo;
import de.itdesigners.winslow.api.pipeline.StageXOrGatewayDefintionInfo;
import de.itdesigners.winslow.config.StageAndGatewayDefinition;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.config.StageWorkerDefinition;
import de.itdesigners.winslow.config.StageXOrGatwayDefinition;

import javax.annotation.Nonnull;
import java.util.Collections;

public class StageDefinitionInfoConverter {

    public static StageDefinitionInfo from(@Nonnull StageDefinition definition) {
        if (definition instanceof StageWorkerDefinition w) {
            return from(w);
        } else if (definition instanceof StageXOrGatwayDefinition x) {
            return from(x);
        } else if (definition instanceof StageAndGatewayDefinition a) {
            return from(a);
        } else {
            throw new RuntimeException("Unsupported StageDefinition " + definition.getClass().getSimpleName());
        }
    }

    @Nonnull
    public static StageWorkerDefinitionInfo from(@Nonnull StageWorkerDefinition definition) {
        return new StageWorkerDefinitionInfo(
                definition.id(),
                definition.name(),
                definition.description(),
                ImageInfoConverter.from(definition.image()),
                RequirementsInfoConverter.from(definition.requirements()),
                UserInputInfoConverter.from(definition.userInput()),
                definition.environment(),
                HighlightInfoConverter.from(definition.highlight()),
                definition.discardable(),
                definition.privileged(),
                Collections.emptyList(),
                definition.ignoreFailuresWithinExecutionGroup(),
                definition.nextStages()
        );
    }

    @Nonnull
    public static StageXOrGatewayDefintionInfo from(@Nonnull StageXOrGatwayDefinition xor) {
        return new StageXOrGatewayDefintionInfo(
                xor.id(),
                xor.name(),
                xor.description(),
                xor.conditions(),
                xor.nextStages()
        );
    }

    @Nonnull
    public static StageAndGatewayDefinitionInfo from(@Nonnull StageAndGatewayDefinition and) {
        return new StageAndGatewayDefinitionInfo(
                and.id(),
                and.name(),
                and.description(),
                and.nextStages()
        );
    }

}
