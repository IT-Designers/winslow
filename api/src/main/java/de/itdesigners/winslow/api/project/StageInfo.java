package de.itdesigners.winslow.api.project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;

public class StageInfo {

    public final @Nullable String              stageId;
    public final @Nullable Date                startTime;
    public final @Nullable Date                finishTime;
    public final @Nullable State               state;
    public final @Nonnull  String              stageName;
    public final @Nullable String              workspace;
    public final @Nonnull  Map<String, String> env;
    public final @Nonnull  Map<String, String> envPipeline;
    public final @Nonnull  Map<String, String> envSystem;
    public final @Nonnull  Map<String, String> envInternal;

    public StageInfo(
            @Nullable String stageId,
            @Nullable Date startTime,
            @Nullable Date finishTime,
            @Nullable State state,
            @Nonnull String stageName,
            @Nullable String workspace,
            @Nonnull Map<String, String> env,
            @Nonnull Map<String, String> envPipeline,
            @Nonnull Map<String, String> envSystem,
            @Nonnull Map<String, String> envInternal) {
        this.stageId                = stageId;
        this.startTime              = startTime;
        this.finishTime             = finishTime;
        this.state                  = state;
        this.stageName              = stageName;
        this.workspace              = workspace;
        this.env                    = env;
        this.envPipeline            = envPipeline;
        this.envSystem              = envSystem;
        this.envInternal            = envInternal;
    }
}
