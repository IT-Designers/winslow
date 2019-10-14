package de.itd.tracking.winslow;

import javax.annotation.Nonnull;
import java.util.*;

public interface PreparedStageBuilder {

    @Nonnull
    String getId();

    @Nonnull
    PreparedStageBuilder withStage(@Nonnull String name);

    @Nonnull
    String getStage();

    @Nonnull
    PreparedStageBuilder withDockerImage(String image);

    @Nonnull
    PreparedStageBuilder withDockerImageArguments(String... args);

    @Nonnull
    PreparedStageBuilder addNfsVolume(
            @Nonnull String volumeName,
            @Nonnull String target,
            boolean readonly,
            @Nonnull String options,
            @Nonnull String serverExport);

    @Nonnull
    Optional<String> getEnvVariable(@Nonnull String key);

    @Nonnull
    PreparedStageBuilder withEnvVariableUnset(@Nonnull String key);

    @Nonnull
    PreparedStageBuilder withEnvVariableSet(@Nonnull String key, @Nonnull String value);

    @Nonnull
    PreparedStageBuilder withEnvVariablesSet(@Nonnull Map<? extends String, ? extends String> variables);

    @Nonnull
    PreparedStageBuilder withGpuCount(int count);

    @Nonnull
    PreparedStageBuilder withGpuVendor(@Nonnull String vendor);

    @Nonnull
    PreparedStage build();
}
