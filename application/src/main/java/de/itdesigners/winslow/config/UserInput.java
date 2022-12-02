package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UserInput {
    private final @Nonnull Confirmation confirmation;
    private final @Nonnull List<String> environment;

    public UserInput(@Nullable Confirmation confirmation, @Nullable List<String> environment) {
        this.confirmation = confirmation != null ? confirmation : Confirmation.Never;
        this.environment  = environment != null ? environment : Collections.emptyList();
    }

    @Nonnull
    public UserInput withoutConfirmation() {
        return new UserInput(
                Confirmation.Never,
                this.environment
        );
    }

    @Nonnull
    public Confirmation getConfirmation() {
        return confirmation != null ? confirmation : Confirmation.Never;
    }

    @Nonnull
    public List<String> getEnvironment() {
        return environment != null ? Collections.unmodifiableList(environment) : Collections.emptyList();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{valueFor=" + this.environment + "}#" + this.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserInput userInput = (UserInput) o;
        return confirmation == userInput.confirmation && Objects.equals(environment, userInput.environment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(confirmation, environment);
    }

    public enum Confirmation {
        Never, Once, Always
    }
}
