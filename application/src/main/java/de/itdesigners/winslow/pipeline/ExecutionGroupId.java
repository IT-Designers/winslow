package de.itdesigners.winslow.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class ExecutionGroupId {

    private final @Nonnull  String projectId;
    private final           int    groupNumberWithinProject;
    private final @Nullable String humanReadableGroupHint;

    public ExecutionGroupId(
            @Nonnull String projectId,
            int groupNumberWithinProject,
            @Nullable String humanReadableGroupHint) {
        this.projectId                = projectId;
        this.groupNumberWithinProject = groupNumberWithinProject;
        this.humanReadableGroupHint   = humanReadableGroupHint;
    }

    @Nonnull
    public String getFullyQualified() {
        return NamedId.buildExecutionGroupId(projectId, groupNumberWithinProject, humanReadableGroupHint);
    }

    @Nonnull
    public String getProjectRelative() {
        return NamedId.buildExecutionGroupId(null, groupNumberWithinProject, humanReadableGroupHint);
    }

    @Nonnull
    public String getProjectId() {
        return projectId;
    }

    public int getGroupNumberWithinProject() {
        return groupNumberWithinProject;
    }

    @Nonnull
    public Optional<String> getHumanReadableGroupHint() {
        return Optional.ofNullable(humanReadableGroupHint);
    }

    @Nonnull
    public StageId generateStageId(@Nullable Integer stageNumberWithinGroup) {
        return new StageId(projectId, groupNumberWithinProject, humanReadableGroupHint, stageNumberWithinGroup);
    }

    @Override
    public String toString() {
        return "ExecutionGroupId{" +
                "projectId='" + projectId + '\'' +
                ", groupNumberWithinProject=" + groupNumberWithinProject +
                ", humanReadableGroupHint='" + humanReadableGroupHint + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ExecutionGroupId that = (ExecutionGroupId) o;
        return getGroupNumberWithinProject() == that.getGroupNumberWithinProject() &&
                getProjectId().equals(that.getProjectId()) &&
                Objects.equals(getHumanReadableGroupHint(), that.getHumanReadableGroupHint());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProjectId(), getGroupNumberWithinProject(), getHumanReadableGroupHint());
    }
}
