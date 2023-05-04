package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.UserInputInfo;
import de.itdesigners.winslow.config.UserInput;

import javax.annotation.Nonnull;

public class UserInputInfoConverter {

    @Nonnull
    public static UserInputInfo from(@Nonnull UserInput input) {
        return new UserInputInfo(
                UserInputInfo.Confirmation.values()[input.getConfirmation().ordinal()],
                input.getRequiredEnvVariables()
        );
    }

    @Nonnull
    public static UserInput reverse(@Nonnull UserInputInfo info) {
        return new UserInput(
                UserInput.Confirmation.values()[info.confirmation().ordinal()],
                info.requiredEnvVariables()
        );
    }

}
