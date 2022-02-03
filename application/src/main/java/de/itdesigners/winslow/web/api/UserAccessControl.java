package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.pipeline.Pipeline;
import de.itdesigners.winslow.project.Project;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.Nonnull;
import java.util.Optional;


public class UserAccessControl {

    private final Winslow winslow;

    public UserAccessControl(Winslow winslow) {
        this.winslow = winslow;
    }

    @Nonnull
    public Optional<Pipeline> getPipelineIfAllowedToAccess(
            @Nonnull User user,
            @PathVariable("projectId") String projectId) {
        return getProjectIfAllowedToAccess(user, projectId)
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getPipeline(project)
                );
    }

    @Nonnull
    public Optional<Project> getProjectIfAllowedToAccess(
            @Nonnull User user,
            @PathVariable("projectId") String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> project.canBeAccessedBy(user));
    }

}
