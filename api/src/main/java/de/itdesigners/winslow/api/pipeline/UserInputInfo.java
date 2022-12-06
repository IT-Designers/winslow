package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import java.util.List;

public record UserInputInfo(
        @Nonnull Confirmation confirmation,
        @Nonnull List<String> environment) {

    public enum Confirmation {
        Never, Once, Always
    }
}
