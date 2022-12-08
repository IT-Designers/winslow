package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UserInput {
    private final @Nonnull Confirmation confirmation;
    private final @Nonnull List<String> requiredEnvVariables;

    public UserInput(@Nullable Confirmation confirmation, @Nullable List<String> requiredEnvVariables) {
        this.confirmation         = confirmation != null ? confirmation : Confirmation.Never;
        this.requiredEnvVariables = requiredEnvVariables != null ? requiredEnvVariables : Collections.emptyList();
    }

    @Nonnull
    public UserInput withoutConfirmation() {
        return new UserInput(
                Confirmation.Never,
                this.requiredEnvVariables
        );
    }

    @Nonnull
    public Confirmation getConfirmation() {
        return confirmation != null ? confirmation : Confirmation.Never;
    }

    @Nonnull
    public List<String> getRequiredEnvVariables() {
        return requiredEnvVariables != null ? Collections.unmodifiableList(requiredEnvVariables) : Collections.emptyList();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{valueFor=" + this.requiredEnvVariables + "}#" + this.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserInput userInput = (UserInput) o;
        return confirmation == userInput.confirmation && Objects.equals(requiredEnvVariables, userInput.requiredEnvVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(confirmation, requiredEnvVariables);
    }

    public enum Confirmation {
        Never, Once, Always
    }
}
