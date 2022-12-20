package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.beans.Transient;
import java.util.Optional;

public record DeletionPolicy(
        boolean keepWorkspaceOfFailedStage,
        boolean alwaysKeepMostRecentWorkspace,
        @Nullable Integer numberOfWorkspacesOfSucceededStagesToKeep) {


    public DeletionPolicy() {
        this(true, true, null);
    }

    @ConstructorProperties({
            "keepWorkspaceOfFailedStage",
            "numberOfWorkspacesOfSucceededStagesToKeep",
            "alwaysKeepMostRecentWorkspace"
    })
    public DeletionPolicy {
    }

    @Nonnull
    @Transient
    public Optional<Integer> optNumberOfWorkspacesOfSucceededStagesToKeep() {
        return Optional.ofNullable(this.numberOfWorkspacesOfSucceededStagesToKeep);
    }

}
