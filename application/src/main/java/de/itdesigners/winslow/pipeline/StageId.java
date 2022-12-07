package de.itdesigners.winslow.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.Transient;
import java.util.Optional;

public class StageId {

    private final @Nonnull  String  projectId;
    private final           int     groupNumberWithinProject;
    private final @Nullable String  humanReadableGroupHint;
    private final @Nullable Integer stageNumberWithinGroup;

    private final @Nonnull String idFullyQualified;
    private final @Nonnull String idProjectRelative;

    public StageId(
            @Nonnull String projectId,
            int groupNumberWithinProject,
            @Nullable String humanReadableGroupHint,
            @Nullable Integer stageNumberWithinGroup) {
        this.projectId                = projectId;
        this.groupNumberWithinProject = groupNumberWithinProject;
        this.humanReadableGroupHint   = humanReadableGroupHint;
        this.stageNumberWithinGroup   = stageNumberWithinGroup;

        this.idFullyQualified = NamedId.buildStageId(
                projectId,
                groupNumberWithinProject,
                humanReadableGroupHint,
                stageNumberWithinGroup
        );

        this.idProjectRelative = NamedId.buildStageId(
                null,
                groupNumberWithinProject,
                humanReadableGroupHint,
                stageNumberWithinGroup
        );
    }

    @Nonnull
    @Transient
    public String getFullyQualified() {
        return idFullyQualified;
    }

    @Nonnull
    @Transient
    public String getProjectRelative() {
        return idProjectRelative;
    }

    @Nonnull
    @Transient
    public ExecutionGroupId getExecutionGroupId() {
        return new ExecutionGroupId(projectId, groupNumberWithinProject, humanReadableGroupHint);
    }

    @Nonnull
    public String getProjectId() {
        return projectId;
    }

    public int getGroupNumberWithinProject() {
        return groupNumberWithinProject;
    }

    @Nonnull
    public Optional<Integer> getStageNumberWithinGroup() {
        return Optional.ofNullable(stageNumberWithinGroup);
    }

    @Nonnull
    public Optional<String> getHumanReadableGroupHint() {
        return Optional.ofNullable(humanReadableGroupHint);
    }
}
