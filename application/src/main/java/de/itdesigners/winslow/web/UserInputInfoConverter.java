package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.StageWorkerDefinitionInfo;
import de.itdesigners.winslow.api.pipeline.UserInputInfo;
import de.itdesigners.winslow.config.StageWorkerDefinition;
import de.itdesigners.winslow.config.UserInput;

import javax.annotation.Nonnull;

public class UserInputInfoConverter {

    public static UserInputInfo from(@Nonnull UserInput input) {
        return new UserInputInfo(
                UserInputInfo.Confirmation.values()[input.getConfirmation().ordinal()],
                input.getEnvironment()
        );
    }


}
