package de.itdesigners.winslow;

import de.itdesigners.winslow.project.Project;

import javax.annotation.Nonnull;

public class PipelineNotFoundException extends OrchestratorException {

    public PipelineNotFoundException(@Nonnull Project project) {
        super("There is no known Pipeline for the Project " + project.getName() + "/" + project.getId());
    }
}
