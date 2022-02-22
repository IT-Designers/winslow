package de.itdesigners.winslow.fs;

import de.itdesigners.winslow.api.pipeline.DeletionPolicy;
import de.itdesigners.winslow.api.pipeline.State;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.config.ExecutionGroup;
import org.springframework.lang.NonNull;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        var obsolete = getAllWorkspaces(this.executionHistory).collect(Collectors.toCollection(ArrayList::new));
        var view     = new WorkspaceView();

        keepAllWorkspacesOfRunningStages(obsolete, view);
        keepMostRecentSuccessfulStagesAndConfigures(obsolete, view);
        // keepAllSuccessfulNonDiscardableWithinRange(obsolete, view);
        keepAllFailedWithinRangeIfKeepIsSet(obsolete, view);
        keepFailedStagesIfPolicyRequiresSo(obsolete, view);

        //appendWorkspaceOfFailedStagesIfApplicable(obsolete);
        //appendWorkspacesOfSuccessfulStagesThatExceedTheLimit(obsolete);
        //appendWorkspacesOfDiscardedStages(obsolete);

        //removeSuccessfullyContinuedWorkspacesButIgnoreOutdatedDiscardable(obsolete);
        //removeDuplicatesKeepReverseOrder(obsolete, Objects::equals);

        return obsolete;
    }

    private int getNumberOfWorkspacesToKeep() {
        return policy
                .getNumberOfWorkspacesOfSucceededStagesToKeep()
                .orElse(Integer.MAX_VALUE);
    }

    private void keepFailedStagesIfPolicyRequiresSo(
            @Nonnull List<String> obsolete,
            @Nonnull WorkspaceView view
    ) {
        if (policy.getKeepWorkspaceOfFailedStage()) {
            for (int i = 0; i < view.distanceToWorkspaces.size() && i < getNumberToKeep(); ++i) {
                var details = view.distanceToWorkspaces.get(i);
                details
                        .stream()
                        .filter(d -> d.notDiscardable)
                        .forEach(d -> obsolete.remove(d.workspace));
            }
        }
    }

    private void keepMostRecentSuccessfulStagesAndConfigures(
            @Nonnull List<String> obsolete,
            @Nonnull WorkspaceView view) {
        boolean foundAtLeastOnceSuccessfulExecution = false;
        int     counterOfKeptSuccessfulStages       = 0;
        for (int i = 0; i < view.distanceToWorkspaces.size() && (!foundAtLeastOnceSuccessfulExecution || counterOfKeptSuccessfulStages < getNumberToKeep()); ++i) {
            var details = view.distanceToWorkspaces.get(i);
            if (details != null) {
                var requireSomeRecentBase = !foundAtLeastOnceSuccessfulExecution;
                var wsToKeep = details
                        .stream()
                        .filter(d -> (requireSomeRecentBase || (d.hasSucceededWithoutDiscardableAtLeastOnce && d.hasExecutedAtLeastOnce)) && d.hasSucceededAtLeastOnce)
                        .collect(Collectors.toList());

                boolean foundSuccessfulStages = wsToKeep
                        .stream()
                        .anyMatch(d -> d.hasExecutedAtLeastOnce);

                foundAtLeastOnceSuccessfulExecution |= foundSuccessfulStages;

                if (foundSuccessfulStages) {
                    if (wsToKeep.stream().anyMatch(d -> d.hasSucceededWithoutDiscardableAtLeastOnce)) {
                        counterOfKeptSuccessfulStages += 1;
                    }
                }

                wsToKeep.forEach(d -> obsolete.remove(d.workspace));
            }
        }
    }

    private void keepAllFailedWithinRangeIfKeepIsSet(
            @Nonnull List<String> obsolete,
            @Nonnull WorkspaceView view) {

        // dont keep failed and dont keep most recent? -> nothing to do
        if (!policy.getKeepWorkspaceOfFailedStage() && !policy.getAlwaysKeepMostRecentWorkspace()) {
            return;
        }

        boolean foundAtLeastOnceSuccessfulExecution                  = false;
        var     potentialDeletionsAndWhetherItHasSuccessfulFollowups = new HashMap<String, Boolean>();
        for (int i = 0; i < view.distanceToWorkspaces.size() && (!foundAtLeastOnceSuccessfulExecution || i < getNumberToKeep()); ++i) {
            var details = view.distanceToWorkspaces.get(i);
            if (details != null) {
                var requireSomeRecentBase = !foundAtLeastOnceSuccessfulExecution;
                details
                        .stream()
                        .filter(d -> (requireSomeRecentBase || d.notDiscardable))
                        .forEach(d -> {
                            var successfulFollowup = potentialDeletionsAndWhetherItHasSuccessfulFollowups.getOrDefault(
                                    d.workspace,
                                    Boolean.FALSE
                            );
                            potentialDeletionsAndWhetherItHasSuccessfulFollowups.put(
                                    d.workspace,
                                    successfulFollowup || d.hasSucceededWithoutDiscardableAtLeastOnce
                            );
                        });

                foundAtLeastOnceSuccessfulExecution |= details
                        .stream()
                        .anyMatch(d -> d.hasSucceededAtLeastOnce);

                if (foundAtLeastOnceSuccessfulExecution && !policy.getKeepWorkspaceOfFailedStage()) {
                    break;
                }

            }
        }

        potentialDeletionsAndWhetherItHasSuccessfulFollowups
                .entrySet()
                .stream()
                .filter(e -> !e.getValue())
                .forEach(e -> obsolete.remove(e.getKey()));
    }

    private void keepAllWorkspacesOfRunningStages(
            @Nonnull List<String> obsolete,
            @Nonnull WorkspaceView view) {

        for (var n = obsolete.size() - 1; n >= 0; --n) {
            var index     = n;
            var workspace = obsolete.get(n);
            var groups    = Optional.ofNullable(view.workspaceLookup.get(workspace));

            groups
                    .stream()
                    .flatMap(Collection::stream)
                    .flatMap(ExecutionGroup::getRunningStages)
                    .flatMap(s -> s.getWorkspace().stream())
                    .filter(w -> w.equals(workspace))
                    .findFirst()
                    .ifPresent(w -> obsolete.remove(index));
        }
    }

    private void keepAllSuccessfulNonDiscardableWithinRange(
            @Nonnull List<String> obsolete,
            @Nonnull WorkspaceView view) {

        int numberToKeep = getNumberToKeep();

        for (var n = obsolete.size() - 1; n >= 0; --n) {
            var index     = n;
            var workspace = obsolete.get(n);
            var groups    = Optional.ofNullable(view.workspaceLookup.get(workspace));
            var distance  = view.getWorkspaceDistance(workspace);

            groups
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(g -> !g.getStageDefinition().isDiscardable())
                    .filter(g -> distance < numberToKeep)
                    .flatMap(ExecutionGroup::getStages)
                    .filter(s -> s.getWorkspace().map(w -> w.equals(workspace)).orElse(Boolean.FALSE))
                    .filter(g -> g.getState() == State.Succeeded)
                    .findFirst()
                    .ifPresent(w -> obsolete.remove(index));
        }
    }

    private Integer getNumberToKeep() {
        return policy.getNumberOfWorkspacesOfSucceededStagesToKeep().orElse(Integer.MAX_VALUE);
    }

    @Nonnull
    private Stream<String> getAllWorkspaces(@Nullable Collection<ExecutionGroup> groups) {
        return Optional
                .ofNullable(groups)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(eg -> {
                    if (eg.getWorkspaceConfiguration().isNestedWithinGroup() && eg.hasRangedValues()) {
                        var paths = eg
                                .getStages()
                                .flatMap(s -> s.getWorkspace().stream())
                                .map(Path::of)
                                .collect(Collectors.toList());

                        var commonParentDirectory = paths.stream().findFirst().flatMap(path -> {
                            var groupDirectory = path.getParent();
                            if (groupDirectory != null && paths.stream().allMatch(p -> p.startsWith(groupDirectory))) {
                                // assuming the same directory
                                return Optional.of(List.of(groupDirectory.toString()));
                            } else {
                                return Optional.empty();
                            }
                        });

                        return commonParentDirectory
                                .orElseGet(() -> paths
                                        .stream()
                                        .map(Path::toString)
                                        .collect(Collectors.toList())
                                )
                                .stream();
                    } else {
                        return eg.getStages().flatMap(s -> s.getWorkspace().stream());
                    }
                })
                .distinct();
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
                if (distance >= numberToKeep) {
                    successfulGroups.get(i).getStages().flatMap(s -> s.getWorkspace().stream()).forEach(obsolete::add);
                } else {
                    break;
                }
            }
        }
    }

    private void appendWorkspacesOfDiscardedStages(@Nonnull List<String> obsolete) {
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
                        .orElse(State.Running) == State.Succeeded) && !group.isConfigureOnly();
            }
        }
    }

    private static class WorkspaceDetail {
        private final @Nonnull String  workspace;
        private                int     distance;
        private                boolean hasSucceededAtLeastOnce;
        private                boolean hasExecutedAtLeastOnce;
        private                boolean notDiscardable;
        private                boolean hasSucceededWithoutDiscardableAtLeastOnce;

        public WorkspaceDetail(@Nonnull String workspace, int distance) {
            this.workspace                                 = workspace;
            this.distance                                  = distance;
            this.hasSucceededAtLeastOnce                   = false;
            this.hasExecutedAtLeastOnce                    = false;
            this.notDiscardable                            = false;
            this.hasSucceededWithoutDiscardableAtLeastOnce = false;
        }
    }

    private class WorkspaceView {
        private final @Nonnull Map<String, WorkspaceDetail>     workspaceDistance    = new HashMap<>();
        private final @Nonnull List<Set<WorkspaceDetail>>       distanceToWorkspaces = new ArrayList<>();
        private final @Nonnull Map<String, Set<ExecutionGroup>> workspaceLookup      = new HashMap<>();

        public WorkspaceView() {
            if (ObsoleteWorkspaceFinder.this.executionHistory != null) {
                for (var group : ObsoleteWorkspaceFinder.this.executionHistory) {
                    group.getStages().forEach(stage -> stage.getWorkspace().ifPresent(w -> {
                        var list = workspaceLookup.getOrDefault(w, new HashSet<>());
                        list.add(group);
                        workspaceLookup.put(w, list);
                    }));
                }


                for (int n = ObsoleteWorkspaceFinder.this.executionHistory.size() - 1; n >= 0; --n) {
                    int distance = ObsoleteWorkspaceFinder.this.executionHistory.size() - n - 1;
                    var groups   = new ArrayList<ExecutionGroup>();

                    groups.add(ObsoleteWorkspaceFinder.this.executionHistory.get(n));

                    // if (distance > numberToKeep) {
                    //     break; // no need to do this precise resolution for older entries
                    // }

                    while (!groups.isEmpty()) {
                        var group = groups.remove(0);

                        group
                                .getStages()
                                .forEach(s -> {
                                    var workspace = s.getWorkspace();

                                    if (group.getWorkspaceConfiguration().isNestedWithinGroup() && group.hasRangedValues()) {
                                        workspace = s.getWorkspace()
                                                .map(Path::of)
                                                .flatMap(p -> Optional.ofNullable(p.getParent()))
                                                .map(Path::toString);
                                    }

                                    workspace.ifPresent(w -> {
                                        var details = workspaceDistance.computeIfAbsent(
                                                w,
                                                ww -> new WorkspaceDetail(w, distance)
                                        );
                                        details.distance = Math.min(details.distance, distance);
                                        details.notDiscardable |= !group.getStageDefinition().isDiscardable();
                                        details.hasSucceededAtLeastOnce |= s.getState() == State.Succeeded;
                                        details.hasExecutedAtLeastOnce |= !group.isConfigureOnly();
                                        details.hasSucceededWithoutDiscardableAtLeastOnce |= !group
                                                .getStageDefinition()
                                                .isDiscardable() && s.getState() == State.Succeeded;
                                    });
                                });
                        ;

                        if (group
                                .getWorkspaceConfiguration()
                                .getMode() == WorkspaceConfiguration.WorkspaceMode.CONTINUATION) {
                            groups.addAll(workspaceLookup.getOrDefault(
                                    group
                                            .getWorkspaceConfiguration()
                                            .getValue()
                                            .orElse(null), Collections.emptySet())
                            );
                        }
                    }


                }

                workspaceDistance
                        .entrySet()
                        .stream()
                        .sorted(Comparator.comparingInt(w -> w.getValue().distance))
                        .forEach(entry -> {
                            while (entry.getValue().distance >= distanceToWorkspaces.size()) {
                                distanceToWorkspaces.add(new HashSet<>());
                            }
                            distanceToWorkspaces.get(entry.getValue().distance).add(entry.getValue());
                        });
            }
        }

        public int getWorkspaceDistance(@Nullable String workspace) {
            return Optional
                    .ofNullable(workspaceDistance.get(workspace))
                    .map(w -> w.distance)
                    .orElse(Integer.MAX_VALUE);
        }
    }
}
