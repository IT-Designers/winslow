package de.itd.tracking.winslow;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Optional;

public class IncompleteStageException extends OrchestratorException {

    @Nullable private final Stage stage;
    @Nullable private final Path  workspace;

    private final boolean requiresConfirmation;
    private final boolean missingEnvVariables;

    private IncompleteStageException(
            @Nonnull String message,
            @Nullable Throwable cause,
            @Nullable Stage stage,
            @Nullable Path workspace,
            boolean requiresConfirmation,
            boolean missingEnvVariables) {
        super(message, cause);
        this.stage                = stage;
        this.workspace            = workspace;
        this.requiresConfirmation = requiresConfirmation;
        this.missingEnvVariables  = missingEnvVariables;
    }

    @Nonnull
    public Optional<Stage> getStage() {
        return Optional.ofNullable(stage);
    }

    @Nonnull
    public Optional<Path> getWorkspace() {
        return Optional.ofNullable(workspace);
    }

    public boolean isConfirmationRequired() {
        return requiresConfirmation;
    }

    public boolean isMissingEnvVariables() {
        return missingEnvVariables;
    }

    public static class Builder {
        @Nonnull private final String     message;
        private                Stage stage;
        private                Path       workspace;
        private                Throwable  cause;
        private                boolean    requiresConfirmation;
        private                boolean    missingEnvVariables;

        private Builder(@Nonnull String message) {
            this.message = message;
        }

        @Nonnull
        @CheckReturnValue
        public static Builder create(String message) {
            return new Builder(message);
        }

        @Nonnull
        @CheckReturnValue
        public Builder withStage(Stage stage) {
            this.stage = stage;
            return this;
        }

        @Nonnull
        @CheckReturnValue
        public Builder withWorkspace(Path workspace) {
            this.workspace = workspace;
            return this;
        }

        @Nonnull
        @CheckReturnValue
        public Builder withCause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        @Nonnull
        @CheckReturnValue
        public Builder maybeRequiresConfirmation(boolean requiresConfirmation) {
            this.requiresConfirmation = requiresConfirmation;
            return this;
        }

        @Nonnull
        @CheckReturnValue
        public Builder requiresConfirmation() {
            this.requiresConfirmation = true;
            return this;
        }

        @Nonnull
        @CheckReturnValue
        public Builder maybeMissingEnvVariables(boolean missingEnvVariables) {
            this.missingEnvVariables = missingEnvVariables;
            return this;
        }

        @Nonnull
        @CheckReturnValue
        public Builder missingEnvVariables() {
            this.missingEnvVariables = true;
            return this;
        }


        @Nonnull
        @CheckReturnValue
        public IncompleteStageException build() {
            return new IncompleteStageException(
                    this.message,
                    this.cause,
                    this.stage,
                    this.workspace,
                    this.requiresConfirmation,
                    this.missingEnvVariables
            );
        }
    }
}
