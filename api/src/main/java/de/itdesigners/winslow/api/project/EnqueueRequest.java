package de.itdesigners.winslow.api.project;

import de.itdesigners.winslow.api.pipeline.ImageInfo;
import de.itdesigners.winslow.api.pipeline.RangedValue;
import de.itdesigners.winslow.api.pipeline.ResourceInfo;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.Transient;
import java.util.Map;
import java.util.Optional;

public record EnqueueRequest(
        @Nonnull String id,
        @Nonnull Map<String, String> env,
        @Nullable Map<String, RangedValue> rangedEnv,
        @Nullable ImageInfo image,
        @Nullable ResourceInfo requiredResources,
        @Nullable WorkspaceConfiguration workspaceConfiguration,
        @Nullable String comment,
        @Nullable Boolean runSingle,
        @Nullable Boolean resume
) {

    @Nonnull
    @Transient
    public Optional<Map<String, RangedValue>> optRangedEnv() {
        return Optional.ofNullable(rangedEnv);
    }

    @Nonnull
    @Transient
    public Optional<ImageInfo> optImage() {
        return Optional.ofNullable(image);
    }

    @Nonnull
    @Transient
    public Optional<ResourceInfo> optRequiredResources() {
        return Optional.ofNullable(requiredResources);
    }

    @Nonnull
    @Transient
    public Optional<WorkspaceConfiguration> optWorkspaceConfiguration() {
        return Optional.ofNullable(workspaceConfiguration);
    }

    @Nonnull
    @Transient
    public Optional<String> optComment() {
        return Optional.ofNullable(comment);
    }

    @Nonnull
    @Transient
    public Optional<Boolean> optRunSingle() {
        return Optional.ofNullable(runSingle);
    }

    @Nonnull
    @Transient
    public Optional<Boolean> optResume() {
        return Optional.ofNullable(resume);
    }

}
