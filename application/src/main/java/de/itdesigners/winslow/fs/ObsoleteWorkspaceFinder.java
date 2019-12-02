package de.itdesigners.winslow.fs;

import de.itdesigners.winslow.pipeline.Action;
import de.itdesigners.winslow.pipeline.DeletionPolicy;
import de.itdesigners.winslow.pipeline.Stage;
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
        appendWorkspacesOfDiscardedStages(obsolete);

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

    private void appendWorkspacesOfDiscardedStages(List<String> obsolete) {
        if (this.executionHistory != null) {
            var hasSuccessfulExecution = false;
            for (int i = this.executionHistory.size() - 1; i >= 0; --i) {
                var stage = this.executionHistory.get(i);

                if (stage.getDefinition().isDiscardable() && hasSuccessfulExecution) {
                    if (stage.getWorkspace().isPresent() && !obsolete.contains(stage.getWorkspace().get())) {
                        obsolete.add(stage.getWorkspace().get());
                    }
                }

                hasSuccessfulExecution |= stage.getAction() == Action.Execute
                        && stage.getFinishState().orElse(Stage.State.Running) == Stage.State.Succeeded;
            }
        }
    }
}
