package de.itdesigners.winslow;

import de.itdesigners.winslow.project.Project;

import javax.annotation.Nonnull;

public class PipelineAlreadyExistsException extends OrchestratorException {

    @Nonnull private final Project project;

    public PipelineAlreadyExistsException(@Nonnull Project project) {
        super("The project with the id " + project.getId() + " alreay has a pipeline");
        this.project = project;
    }

    @Nonnull
    public Project getProject() {
        return project;
    }
}
