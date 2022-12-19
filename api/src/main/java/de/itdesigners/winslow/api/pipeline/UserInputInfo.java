package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import java.util.List;

public record UserInputInfo(
        @Nonnull Confirmation confirmation,
        @Nonnull List<String> requiredEnvVariables) {

    public enum Confirmation {
        NEVER, ONCE, ALWAYS
    }
}
