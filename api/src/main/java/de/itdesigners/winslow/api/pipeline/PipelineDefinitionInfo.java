package de.itdesigners.winslow.api.pipeline;

import de.itdesigners.winslow.api.auth.Link;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

public record PipelineDefinitionInfo(
        @Nonnull String id,
        @Nonnull String name,
        @Nonnull String description,
        @Nonnull UserInputInfo userInput,
        @Nonnull List<StageDefinitionInfo> stages,
        @Nonnull Map<String, String> environment,
        @Nonnull DeletionPolicy deletionPolicy,
        @Nonnull List<String> markers,
        @Nonnull List<Link> groups,
        boolean publicAccess) {

    public PipelineDefinitionInfo(
            @Nonnull String id,
            @Nonnull String name,
            @Nonnull String description,
            @Nonnull UserInputInfo userInput,
            @Nonnull List<StageDefinitionInfo> stages,
            @Nonnull Map<String, String> environment,
            @Nonnull DeletionPolicy deletionPolicy,
            @Nonnull List<String> markers,
            @Nonnull List<Link> groups,
            boolean publicAccess) {
        this.id             = id;
        this.name           = name;
        this.description    = description;
        this.userInput      = userInput;
        this.stages         = stages;
        this.environment    = environment;
        this.deletionPolicy = deletionPolicy;
        this.markers        = markers;
        this.groups         = groups;
        this.publicAccess   = publicAccess;
    }
}
