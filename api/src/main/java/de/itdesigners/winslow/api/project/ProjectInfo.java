package de.itdesigners.winslow.api.project;

import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.pipeline.PipelineInfo;

import javax.annotation.Nonnull;
import java.util.List;

public class ProjectInfo {

    public final @Nonnull String       id;
    public final @Nonnull String       owner;
    public final @Nonnull List<Link>   groups;
    public final @Nonnull List<String> tags;
    public final @Nonnull String       name;
    public final          boolean      publicAccess;
    public final @Nonnull PipelineInfo pipelineDefinition;

    public ProjectInfo(
            @Nonnull String id,
            @Nonnull String owner,
            @Nonnull List<Link> groups,
            @Nonnull List<String> tags,
            @Nonnull String name,
            boolean publicAccess,
            @Nonnull PipelineInfo pipelineDefinition) {
        this.id                 = id;
        this.owner              = owner;
        this.groups             = groups;
        this.tags               = tags;
        this.name               = name;
        this.publicAccess       = publicAccess;
        this.pipelineDefinition = pipelineDefinition;
    }
}
