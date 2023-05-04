package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.StageAndGatewayDefinitionInfo;
import de.itdesigners.winslow.api.pipeline.StageDefinitionInfo;
import de.itdesigners.winslow.api.pipeline.StageWorkerDefinitionInfo;
import de.itdesigners.winslow.api.pipeline.StageXOrGatewayDefinitionInfo;
import de.itdesigners.winslow.config.StageAndGatewayDefinition;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.config.StageWorkerDefinition;
import de.itdesigners.winslow.config.StageXOrGatewayDefinition;

import javax.annotation.Nonnull;

public class StageDefinitionInfoConverter {

    @Nonnull
    public static StageDefinitionInfo from(@Nonnull StageDefinition definition) {
        if (definition instanceof StageWorkerDefinition w) {
            return from(w);
        } else if (definition instanceof StageXOrGatewayDefinition x) {
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
                definition.logParsers().stream().map(LogParserInfoConverter::from).toList(),
                definition.ignoreFailuresWithinExecutionGroup(),
                definition.nextStages()
        );
    }

    @Nonnull
    public static StageXOrGatewayDefinitionInfo from(@Nonnull StageXOrGatewayDefinition xor) {
        return new StageXOrGatewayDefinitionInfo(
                xor.id(),
                xor.name(),
                xor.description(),
                xor.conditions(),
                xor.nextStages(),
                xor.gatewaySubType()
        );
    }

    @Nonnull
    public static StageAndGatewayDefinitionInfo from(@Nonnull StageAndGatewayDefinition and) {
        return new StageAndGatewayDefinitionInfo(
                and.id(),
                and.name(),
                and.description(),
                and.nextStages(),
                and.gatewaySubType()
        );
    }

    @Nonnull
    public static StageDefinition reverse(@Nonnull StageDefinitionInfo definition) {
        if (definition instanceof StageWorkerDefinitionInfo w) {
            return reverse(w);
        } else if (definition instanceof StageXOrGatewayDefinitionInfo x) {
            return reverse(x);
        } else if (definition instanceof StageAndGatewayDefinitionInfo a) {
            return reverse(a);
        } else {
            throw new RuntimeException("Unsupported StageDefinition " + definition.getClass().getSimpleName());
        }
    }


    @Nonnull
    public static StageWorkerDefinition reverse(@Nonnull StageWorkerDefinitionInfo info) {
        return new StageWorkerDefinition(
                info.id(),
                info.name(),
                info.description(),
                info.nextStages(),
                ImageInfoConverter.reverse(info.image()),
                RequirementsInfoConverter.reverse(info.requiredResources()),
                UserInputInfoConverter.reverse(info.userInput()),
                info.environment(),
                HighlightInfoConverter.reverse(info.highlight()),
                info.logParsers().stream().map(LogParserInfoConverter::reverse).toList(),
                info.discardable(),
                info.privileged(),
                info.ignoreFailuresWithinExecutionGroup()
        );
    }

    @Nonnull
    public static StageXOrGatewayDefinition reverse(@Nonnull StageXOrGatewayDefinitionInfo info) {
        return new StageXOrGatewayDefinition(
                info.id(),
                info.name(),
                info.description(),
                info.conditions(),
                info.nextStages(),
                info.gatewaySubType()
        );
    }

    @Nonnull
    public static StageAndGatewayDefinition reverse(@Nonnull StageAndGatewayDefinitionInfo info) {
        return new StageAndGatewayDefinition(
                info.id(),
                info.name(),
                info.description(),
                info.nextStages(),
                info.gatewaySubType()
        );
    }
}
