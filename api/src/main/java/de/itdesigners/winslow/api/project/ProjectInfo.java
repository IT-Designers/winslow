package de.itdesigners.winslow.api.project;

import de.itdesigners.winslow.api.auth.Link;
import de.itdesigners.winslow.api.pipeline.PipelineDefinitionInfo;

import javax.annotation.Nonnull;
import java.util.List;

public record ProjectInfo(
        @Nonnull String id,
        @Nonnull String accountingGroup,
        @Nonnull List<Link> groups,
        @Nonnull List<String> tags,
        @Nonnull String name,
        boolean publicAccess,
        @Nonnull PipelineDefinitionInfo pipelineDefinition) {

}
