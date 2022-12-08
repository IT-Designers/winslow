package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.project.ProjectInfo;
import de.itdesigners.winslow.project.Project;

import javax.annotation.Nonnull;

public class ProjectInfoConverter {

    @Nonnull
    public static ProjectInfo from(@Nonnull Project project) {
        return new ProjectInfo(
                project.getId(),
                project.getOwner(),
                project.getGroups(),
                project.getTags(),
                project.getName(),
                project.isPublic(),
                PipelineDefinitionInfoConverter.from(
                        project.getId(),
                        project.getPipelineDefinition()
                )
        );
    }
}
