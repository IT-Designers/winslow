package de.itdesigners.winslow.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.Transient;
import java.util.Objects;
import java.util.Optional;

public class StageId {

    private final @Nonnull  String  projectId;
    private final           int     groupNumberWithinProject;
    private final @Nullable String  humanReadableGroupHint;
    private final @Nullable Integer stageNumberWithinGroup;

    private @Nullable String idFullyQualified;
    private @Nullable String idProjectRelative;

    public StageId(
            @Nonnull String projectId,
            int groupNumberWithinProject,
            @Nullable String humanReadableGroupHint,
            @Nullable Integer stageNumberWithinGroup) {
        this.projectId                = projectId;
        this.groupNumberWithinProject = groupNumberWithinProject;
        this.humanReadableGroupHint   = humanReadableGroupHint;
        this.stageNumberWithinGroup   = stageNumberWithinGroup;
    }

    @Nonnull
    @Transient
    public String getFullyQualified() {
        if (this.idFullyQualified == null) {
            this.idFullyQualified = NamedId.buildStageId(
                    projectId,
                    groupNumberWithinProject,
                    humanReadableGroupHint,
                    stageNumberWithinGroup
            );
        }
        return this.idFullyQualified;
    }

    @Nonnull
    @Transient
    public String getProjectRelative() {
        if (this.idProjectRelative == null) {
            this.idProjectRelative = NamedId.buildStageId(
                    null,
                    groupNumberWithinProject,
                    humanReadableGroupHint,
                    stageNumberWithinGroup
            );
        }
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

    @Override
    public String toString() {
        return "StageId{" +
                "projectId='" + projectId + '\'' +
                ", groupNumberWithinProject=" + groupNumberWithinProject +
                ", humanReadableGroupHint='" + humanReadableGroupHint + '\'' +
                ", stageNumberWithinGroup=" + stageNumberWithinGroup +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        StageId stageId = (StageId) o;
        return getGroupNumberWithinProject() == stageId.getGroupNumberWithinProject() &&
                getProjectId().equals(stageId.getProjectId()) &&
                Objects.equals(getHumanReadableGroupHint(), stageId.getHumanReadableGroupHint()) &&
                Objects.equals(getStageNumberWithinGroup(), stageId.getStageNumberWithinGroup());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getProjectId(),
                getGroupNumberWithinProject(),
                getHumanReadableGroupHint(),
                getStageNumberWithinGroup()
        );
    }
}
