package de.itd.tracking.winslow.fs;

import de.itd.tracking.winslow.pipeline.Action;
import de.itd.tracking.winslow.pipeline.DeletionPolicy;
import de.itd.tracking.winslow.pipeline.Stage;
import org.springframework.lang.NonNull;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ObsoleteWorkspaceFinder {

    private @NonNull final DeletionPolicy policy;
    private @Nullable      List<Stage>    executionHistory;

    public ObsoleteWorkspaceFinder(@Nonnull DeletionPolicy policy) {
        this.policy = policy;
    }

    @NonNull
    @CheckReturnValue
    public ObsoleteWorkspaceFinder withExecutionHistory(@Nullable List<Stage> history) {
        this.executionHistory = history;
        return this;
    }

    @NonNull
    @CheckReturnValue
    public List<String> collectObsoleteWorkspaces() {
        var obsolete = new ArrayList<String>();

        appendWorkspaceOfFailedStagesIfApplicable(obsolete);
        appendWorkspacesOfSuccessfulStagesThatExceedTheLimit(obsolete);

        return obsolete;
    }

    private void appendWorkspaceOfFailedStagesIfApplicable(@NonNull List<String> obsolete) {
        if (!policy.getKeepWorkspaceOfFailedStage() && this.executionHistory != null) {
            this.executionHistory
                    .stream()
                    .filter(stage -> stage.getFinishState().orElse(Stage.State.Running) == Stage.State.Failed)
                    .filter(stage -> stage.getAction() == Action.Execute)
                    .filter(stage -> stage.getWorkspace().isPresent())
                    .forEach(stage -> obsolete.add(stage.getWorkspace().get()));
        }
    }

    private void appendWorkspacesOfSuccessfulStagesThatExceedTheLimit(@NonNull List<String> obsolete) {
        if (policy.getNumberOfWorkspacesOfSucceededStagesToKeep().isPresent() && this.executionHistory != null) {
            var numberToKeep = policy.getNumberOfWorkspacesOfSucceededStagesToKeep().get();
            var successfulStages = this.executionHistory
                    .stream()
                    .filter(stage -> stage.getFinishState().orElse(Stage.State.Running) == Stage.State.Succeeded)
                    .filter(stage -> stage.getAction() == Action.Execute)
                    .collect(Collectors.toList());

            for (int i = 0; i < successfulStages.size(); ++i) {
                var distance = successfulStages.size() - i;
                if (distance > numberToKeep) {
                    successfulStages.get(i).getWorkspace().ifPresent(obsolete::add);
                } else {
                    break;
                }
            }
        }
    }
}
