package de.itd.tracking.winslow.nomad;

import de.itd.tracking.winslow.OrchestratorException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Optional;

public class IncompleteStageException extends OrchestratorException {

    @Nullable private final NomadStage stage;
    @Nullable private final  Path       workspace;

    public IncompleteStageException(@Nullable NomadStage stage, @Nullable Path workspace, String message) {
        super(message);
        this.stage     = stage;
        this.workspace = workspace;
    }

    public IncompleteStageException(@Nullable NomadStage stage, @Nullable Path workspace, String message, Throwable cause) {
        super(message, cause);
        this.stage     = stage;
        this.workspace = workspace;
    }

    @Nonnull
    public Optional<NomadStage> getStage() {
        return Optional.ofNullable(stage);
    }

    @Nonnull
    public Optional<Path> getWorkspace() {
        return Optional.ofNullable(workspace);
    }
}
