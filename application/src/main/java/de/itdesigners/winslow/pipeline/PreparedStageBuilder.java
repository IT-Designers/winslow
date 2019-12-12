package de.itdesigners.winslow.pipeline;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;

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
    Iterable<String> getEnvVariableKeys();

    @Nonnull
    Optional<String> getEnvVariable(@Nonnull String key);

    @Nonnull
    PreparedStageBuilder withInternalEnvVariable(@Nonnull String key, @Nonnull String value);

    @Nonnull
    PreparedStageBuilder withPipelineEnvVariables(@Nonnull Map<? extends String, ? extends String> variables);

    @Nonnull
    PreparedStageBuilder withSystemEnvVariables(@Nonnull Map<? extends String, ? extends String> variables);

    @Nonnull
    PreparedStageBuilder withEnvVariables(@Nonnull Map<? extends String, ? extends String> variables);

    @Nonnull
    PreparedStageBuilder withCpuCount(int count);

    @Nonnull
    PreparedStageBuilder withGpuCount(int count);

    @Nonnull
    PreparedStageBuilder withGpuVendor(@Nonnull String vendor);

    @Nonnull
    PreparedStageBuilder withMegabytesOfRam(int megabytesOfRam);

    @Nonnull
    PreparedStage build();

    @Nonnull
    PreparedStageBuilder withWorkspaceDirectory(@Nonnull String viewAsPipeline);
}
