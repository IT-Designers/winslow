package de.itdesigners.winslow.api.pipeline;

import de.itdesigners.winslow.api.auth.Link;

import javax.annotation.Nonnull;
import java.util.List;

public record PipelineDefinitionInfo(
        @Nonnull String id,
        @Nonnull String name,
        @Nonnull String description,
        @Nonnull UserInputInfo userInput,
        @Nonnull List<StageDefinitionInfo> stages,
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
            @Nonnull DeletionPolicy deletionPolicy,
            @Nonnull List<String> markers,
            @Nonnull List<Link> groups,
            boolean publicAccess) {
        this.id             = id;
        this.name           = name;
        this.description    = description;
        this.userInput      = userInput;
        this.stages         = stages;
        this.deletionPolicy = deletionPolicy;
        this.markers        = markers;
        this.groups         = groups;
        this.publicAccess   = publicAccess;
    }
}
