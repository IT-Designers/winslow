package de.itdesigners.winslow.web;

import de.itdesigners.winslow.PipelineDefinitionRepository;
import de.itdesigners.winslow.api.project.ProjectInfo;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.project.Project;

import javax.annotation.Nonnull;

public class ProjectInfoConverter {

    @Nonnull
    public static ProjectInfo from(@Nonnull Project project, @Nonnull PipelineDefinition pipelineDefinition) {
        return new ProjectInfo(
                project.getId(),
                project.getAccountingGroup(),
                project.getGroups(),
                project.getTags(),
                project.getName(),
                project.isPublic(),
                PipelineDefinitionInfoConverter.from(pipelineDefinition)
        );
    }
}
