package de.itdesigners.winslow.api.project;

import de.itdesigners.winslow.api.pipeline.ImageInfo;
import de.itdesigners.winslow.api.pipeline.RangedValue;
import de.itdesigners.winslow.api.pipeline.ResourceInfo;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class EnqueueRequest {
    public @Nonnull  Map<String, String>      env;
    public @Nullable Map<String, RangedValue> rangedEnv;
    public           int                      stageIndex;
    public @Nullable ImageInfo                image;
    public @Nullable ResourceInfo             requiredResources;
    public @Nullable WorkspaceConfiguration   workspaceConfiguration;
    public @Nullable String                   comment;
    public @Nullable Boolean                  runSingle;
    public @Nullable Boolean                  resume;

    public EnqueueRequest(
            @Nonnull Map<String, String> env,
            @Nullable Map<String, RangedValue> rangedEnv,
            int stageIndex,
            @Nullable ImageInfo image,
            @Nullable ResourceInfo requiredResources,
            @Nullable WorkspaceConfiguration workspaceConfiguration,
            @Nullable String comment,
            @Nullable Boolean runSingle, @Nullable Boolean resume) {
        this.env                    = env;
        this.rangedEnv              = rangedEnv;
        this.stageIndex             = stageIndex;
        this.image                  = image;
        this.requiredResources      = requiredResources;
        this.workspaceConfiguration = workspaceConfiguration;
        this.comment                = comment;
        this.runSingle              = runSingle;
        this.resume                 = resume;
    }



}
