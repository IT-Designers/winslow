package de.itdesigners.winslow.api.project;

import de.itdesigners.winslow.api.pipeline.ImageInfo;
import de.itdesigners.winslow.api.pipeline.RangedValue;
import de.itdesigners.winslow.api.pipeline.ResourceInfo;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class EnqueueOnOtherRequest extends EnqueueRequest {
    public @Nonnull String[] projectIds;

    public EnqueueOnOtherRequest(
            @Nonnull Map<String, String> env,
            @Nullable Map<String, RangedValue> rangedEnv,
            int stageIndex,
            @Nullable ImageInfo image,
            @Nullable ResourceInfo requiredResources,
            @Nullable WorkspaceConfiguration workspaceConfiguration,
            @Nullable String comment,
            @Nullable Boolean runSingle, @Nullable Boolean resume,
            @Nonnull String[] projectIds) {
        super(env, rangedEnv, stageIndex, image, requiredResources, workspaceConfiguration, comment, runSingle, resume);
        this.projectIds = projectIds;
    }
}
