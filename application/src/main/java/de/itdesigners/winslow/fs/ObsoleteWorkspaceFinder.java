package de.itdesigners.winslow.fs;

import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.api.project.DeletionPolicy;
import de.itdesigners.winslow.api.project.State;
import de.itdesigners.winslow.pipeline.Stage;
import org.springframework.lang.NonNull;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
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

        removeSuccessfullyContinuedWorkspacesButIgnoreOutdatedDiscardable(obsolete);
        removeDuplicates(obsolete, Objects::equals);

        return obsolete;
    }

    private <T> void removeDuplicates(List<T> obsolete, BiFunction<T, T, Boolean> c) {
        for (int i = 0; i < obsolete.size(); ++i) {
            for (int n = obsolete.size() - 1; n > i; --n) {
                if (c.apply(obsolete.get(i), obsolete.get(n))) {
                    obsolete.remove(n);
                }
            }
        }
    }

    private void removeSuccessfullyContinuedWorkspacesButIgnoreOutdatedDiscardable(List<String> obsolete) {
        int numberToKeep = policy.getNumberOfWorkspacesOfSucceededStagesToKeep().orElse(Integer.MAX_VALUE);
        var successfulStages = Optional
                .ofNullable(this.executionHistory)
                .stream()
                .flatMap(Collection::stream)
                .filter(stage -> stage.getFinishState().orElse(State.Running) == State.Succeeded)
                .filter(stage -> stage.getAction() == Action.Execute)
                .collect(Collectors.toList());


        Map<String, Boolean> workspaceDiscardable = new HashMap<>();

        for (var stage : successfulStages) {
            var workspace   = stage.getWorkspace().orElse(null);
            var discardable = workspaceDiscardable.getOrDefault(workspace, Boolean.TRUE);
            workspaceDiscardable.put(workspace, discardable && stage.getDefinition().isDiscardable());
        }

        var successfulWorkspaces = successfulStages
                .stream()
                .map(Stage::getWorkspace)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        removeDuplicates(successfulWorkspaces, String::equals);


        for (int i = 0; i < numberToKeep && i < successfulWorkspaces.size(); ++i) {
            var workspace   = successfulWorkspaces.get(successfulWorkspaces.size() - i - 1);
            var discardable = workspaceDiscardable.get(workspace);

            // only skip discardable from whitelisting if it is the very first item
            if (discardable && i > 0) {
                numberToKeep++; // do not prevent deletion
            } else {
                while (obsolete.remove(workspace))
                    ;
            }
        }
    }

    private void appendWorkspaceOfFailedStagesIfApplicable(@NonNull List<String> obsolete) {
        if (!policy.getKeepWorkspaceOfFailedStage() && this.executionHistory != null) {
            this.executionHistory
                    .stream()
                    .filter(stage -> stage.getFinishState().orElse(State.Running) == State.Failed)
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
                    .filter(stage -> stage.getFinishState().orElse(State.Running) == State.Succeeded)
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
                        && stage.getFinishState().orElse(State.Running) == State.Succeeded;
            }
        }
    }
}
