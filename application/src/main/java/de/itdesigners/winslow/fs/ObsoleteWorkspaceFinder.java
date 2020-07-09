package de.itdesigners.winslow.fs;

import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.api.pipeline.DeletionPolicy;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.config.ExecutionGroup;
import org.springframework.lang.NonNull;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ObsoleteWorkspaceFinder {

    private @NonNull final DeletionPolicy       policy;
    private @Nullable      List<ExecutionGroup> executionHistory;

    public ObsoleteWorkspaceFinder(@Nonnull DeletionPolicy policy) {
        this.policy = policy;
    }

    @NonNull
    @CheckReturnValue
    public ObsoleteWorkspaceFinder withExecutionHistory(@Nullable List<ExecutionGroup> history) {
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
        removeDuplicatesKeepReverseOrder(obsolete, Objects::equals);

        return obsolete;
    }

    protected static <T> void removeDuplicatesKeepReverseOrder(List<T> list, BiFunction<T, T, Boolean> c) {
        for (int i = list.size() - 1; i >= 0; --i) {
            for (int n = i - 1; n >= 0; --n) {
                if (c.apply(list.get(i), list.get(n))) {
                    list.remove(n);
                    i -= 1;
                }
            }
        }
    }

    private void removeSuccessfullyContinuedWorkspacesButIgnoreOutdatedDiscardable(List<String> obsolete) {
        if (this.executionHistory != null) {
            int numberToKeep = policy.getNumberOfWorkspacesOfSucceededStagesToKeep().orElse(Integer.MAX_VALUE);

            var workspaceDistance = new HashMap<String, Integer>();
            var workspaceLookup   = new HashMap<String, ExecutionGroup>();

            for (var group : executionHistory) {
                group.getStages().forEach(stage -> stage.getWorkspace().ifPresent(w -> workspaceLookup.put(w, group)));
            }


            for (int n = this.executionHistory.size() - 1; n >= 0; --n) {
                int distance = this.executionHistory.size() - n;
                var group    = this.executionHistory.get(n);

                if (distance > numberToKeep) {
                    break; // no need to do this precise resolution for older entries
                }

                while (group != null) {
                    group
                            .getStages()
                            .flatMap(s -> s.getWorkspace().stream())
                            .forEach(w -> workspaceDistance.put(w, distance));

                    if (group
                            .getWorkspaceConfiguration()
                            .getMode() == WorkspaceConfiguration.WorkspaceMode.CONTINUATION) {
                        group = workspaceLookup.get(group
                                                            .getWorkspaceConfiguration()
                                                            .getValue()
                                                            .orElse(null));
                    } else {
                        group = null;
                    }
                }

            }

            for (int n = obsolete.size() - 1; n >= 0; --n) {
                if (workspaceDistance.getOrDefault(obsolete.get(n), Integer.MAX_VALUE) > numberToKeep) {
                    obsolete.remove(n);
                }
            }
        }
    }


    private void appendWorkspaceOfFailedStagesIfApplicable(@NonNull List<String> obsolete) {
        if (!policy.getKeepWorkspaceOfFailedStage() && this.executionHistory != null) {
            this.executionHistory
                    .stream()
                    .flatMap(ExecutionGroup::getStages)
                    .filter(stage -> stage.getFinishState().orElse(State.Running) == State.Failed)
                    .flatMap(stage -> stage.getWorkspace().stream())
                    .forEach(obsolete::add);
        }
    }

    private void appendWorkspacesOfSuccessfulStagesThatExceedTheLimit(@NonNull List<String> obsolete) {
        if (policy.getNumberOfWorkspacesOfSucceededStagesToKeep().isPresent() && this.executionHistory != null) {
            int numberToKeep = policy.getNumberOfWorkspacesOfSucceededStagesToKeep().get();
            var successfulGroups = this.executionHistory
                    .stream()
                    .filter(group -> group
                            .getStages()
                            .anyMatch(s -> s.getFinishState().orElse(State.Running) == State.Succeeded))
                    .collect(Collectors.toList());

            for (int i = 0; i < successfulGroups.size(); ++i) {
                var distance = successfulGroups.size() - i;
                if (distance > numberToKeep) {
                    successfulGroups.get(i).getStages().flatMap(s -> s.getWorkspace().stream()).forEach(obsolete::add);
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
                var group = this.executionHistory.get(i);

                if (group.getStageDefinition().isDiscardable() && hasSuccessfulExecution) {
                    group.getCompletedStages()
                         .flatMap(s -> s.getWorkspace().stream())
                         .filter(w -> !obsolete.contains(w))
                         .forEach(obsolete::add);
                }

                hasSuccessfulExecution |= group.getCompletedStages().anyMatch(s -> s
                        .getFinishState()
                        .orElse(State.Running) == State.Succeeded);
            }
        }
    }
}
