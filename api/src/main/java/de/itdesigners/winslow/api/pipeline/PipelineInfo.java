package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class PipelineInfo {

    public final @Nonnull  String          id;
    public final @Nonnull  String          name;
    public final @Nullable String          desc;
    public final @Nonnull  List<String>    requiredEnvVariables;
    public final @Nonnull  List<StageInfo> stages;
    public final @Nonnull  List<String>    markers;

    public PipelineInfo(
            @Nonnull String id,
            @Nonnull String name,
            @Nullable String desc,
            @Nonnull List<String> requiredEnvVariables,
            @Nonnull List<StageInfo> stages,
            @Nonnull List<String> markers) {
        this.id                   = id;
        this.name                 = name;
        this.desc                 = desc;
        this.requiredEnvVariables = requiredEnvVariables;
        this.stages               = stages;
        this.markers              = markers;
    }
}
