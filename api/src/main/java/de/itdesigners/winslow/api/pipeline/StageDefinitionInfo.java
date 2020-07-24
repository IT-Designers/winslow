package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class StageDefinitionInfo {

    public final @Nonnull  String              name;
    public final @Nullable ImageInfo           image;
    public final @Nonnull  List<String>        requiredEnvVariables;
    public final @Nullable ResourceInfo        requiredResources;
    public final @Nullable Map<String, String> env;

    public StageDefinitionInfo(
            @Nonnull String name,
            @Nullable ImageInfo image,
            @Nonnull List<String> requiredEnvVariables,
            @Nullable ResourceInfo requiredResources,
            @Nullable Map<String, String> env) {
        this.name                 = name;
        this.image                = image;
        this.requiredEnvVariables = requiredEnvVariables;
        this.requiredResources    = requiredResources;
        this.env                  = env;
    }
}
