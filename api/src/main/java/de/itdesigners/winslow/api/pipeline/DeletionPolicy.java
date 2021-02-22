package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.Optional;

public class DeletionPolicy {

    private           boolean keepWorkspaceOfFailedStage;
    private @Nullable Integer numberOfWorkspacesOfSucceededStagesToKeep;
    private           boolean alwaysKeepMostRecentWorkspace;

    public DeletionPolicy() {
        this(true, null);
    }

    public DeletionPolicy(
            boolean keepWorkspaceOfFailedStage,
            @Nullable Integer numberOfWorkspacesOfSucceededStagesToKeep) {
        this(keepWorkspaceOfFailedStage, numberOfWorkspacesOfSucceededStagesToKeep, null);
    }

    @ConstructorProperties({
            "keepWorkspaceOfFailedStage",
            "numberOfWorkspacesOfSucceededStagesToKeep",
            "alwaysKeepMostRecentWorkspace"
    })
    public DeletionPolicy(
            boolean keepWorkspaceOfFailedStage,
            @Nullable Integer numberOfWorkspacesOfSucceededStagesToKeep,
            @Nullable Boolean alwaysKeepMostRecentWorkspace) {
        this.keepWorkspaceOfFailedStage                = keepWorkspaceOfFailedStage;
        this.numberOfWorkspacesOfSucceededStagesToKeep = numberOfWorkspacesOfSucceededStagesToKeep;
        this.alwaysKeepMostRecentWorkspace             = alwaysKeepMostRecentWorkspace != null
                                                         ? alwaysKeepMostRecentWorkspace
                                                         : true;
    }

    public boolean getKeepWorkspaceOfFailedStage() {
        return keepWorkspaceOfFailedStage;
    }

    public void setKeepWorkspaceOfFailedStage(boolean keepWorkspaceOfFailedStage) {
        this.keepWorkspaceOfFailedStage = keepWorkspaceOfFailedStage;
    }

    public Optional<Integer> getNumberOfWorkspacesOfSucceededStagesToKeep() {
        return Optional.ofNullable(this.numberOfWorkspacesOfSucceededStagesToKeep);
    }

    public void setNumberOfWorkspacesOfSucceededStagesToKeep(@Nullable Integer numberOfWorkspacesOfSucceededStagesToKeep) {
        this.numberOfWorkspacesOfSucceededStagesToKeep = numberOfWorkspacesOfSucceededStagesToKeep;
    }

    public boolean getAlwaysKeepMostRecentWorkspace() {
        return this.alwaysKeepMostRecentWorkspace;
    }

    public void setAlwaysKeepMostRecentWorkspace(boolean alwaysKeep) {
        this.alwaysKeepMostRecentWorkspace = alwaysKeep;
    }
}
