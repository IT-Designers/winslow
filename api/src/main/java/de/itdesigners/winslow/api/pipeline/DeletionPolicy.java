package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nullable;
import java.util.Optional;

public class DeletionPolicy {

    private           boolean keepWorkspaceOfFailedStage;
    private @Nullable Integer numberOfWorkspacesOfSucceededStagesToKeep;

    public DeletionPolicy() {
        this(true, null);
    }

    public DeletionPolicy(
            boolean keepWorkspaceOfFailedStage,
            @Nullable Integer numberOfWorkspacesOfSucceededStagesToKeep) {
        this.keepWorkspaceOfFailedStage                = keepWorkspaceOfFailedStage;
        this.numberOfWorkspacesOfSucceededStagesToKeep = numberOfWorkspacesOfSucceededStagesToKeep;
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
}
