package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class StageInfo {
    public final @Nonnull  String       name;
    public final @Nullable ImageInfo    image;
    public final @Nonnull  List<String> requiredEnvVariables;

    public StageInfo(@Nonnull String name, @Nullable ImageInfo image, @Nonnull List<String> requiredEnvVariables) {
        this.name                 = name;
        this.image                = image;
        this.requiredEnvVariables = requiredEnvVariables;
    }
}
