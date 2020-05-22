package de.itdesigners.winslow.api.project;

import de.itdesigners.winslow.api.pipeline.Action;
import de.itdesigners.winslow.api.pipeline.ImageInfo;
import de.itdesigners.winslow.api.pipeline.ResourceInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;

public class HistoryEntry {

    public final @Nullable String              stageId;
    public final @Nullable Date                startTime;
    public final @Nullable Date                finishTime;
    public final @Nullable State               state;
    public final @Nonnull  Action              action;
    public final @Nonnull  String              stageName;
    public final @Nullable String              workspace;
    public final @Nullable ImageInfo           imageInfo;
    public final @Nonnull  Map<String, String> env;
    public final @Nonnull  Map<String, String> envPipeline;
    public final @Nonnull  Map<String, String> envSystem;
    public final @Nonnull  Map<String, String> envInternal;
    public final @Nullable ResourceInfo        resourceRequirements;

    public HistoryEntry(
            @Nullable String stageId,
            @Nullable Date startTime,
            @Nullable Date finishTime,
            @Nullable State state,
            @Nonnull Action action,
            @Nonnull String stageName,
            @Nullable String workspace,
            @Nullable ImageInfo imageInfo,
            @Nonnull Map<String, String> env,
            @Nonnull Map<String, String> envPipeline,
            @Nonnull Map<String, String> envSystem,
            @Nonnull Map<String, String> envInternal,
            @Nullable ResourceInfo resourceRequirements) {
        this.stageId              = stageId;
        this.startTime            = startTime;
        this.finishTime           = finishTime;
        this.state                = state;
        this.action               = action;
        this.stageName            = stageName;
        this.workspace            = workspace;
        this.imageInfo            = imageInfo;
        this.env                  = env;
        this.envPipeline          = envPipeline;
        this.envSystem            = envSystem;
        this.envInternal          = envInternal;
        this.resourceRequirements = resourceRequirements;
    }
}
