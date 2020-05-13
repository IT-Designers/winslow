package de.itdesigners.winslow.web;

import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.project.Project;
import de.itdesigners.winslow.resource.ResourceManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

public class FileAccessChecker {

    private final @Nonnull ResourceManager                     resourceManager;
    private final @Nonnull Function<String, Optional<Project>> projectResolver;

    public FileAccessChecker(
            @Nonnull ResourceManager resourceManager,
            @Nonnull Function<String, Optional<Project>> projectResolver) {
        this.resourceManager = resourceManager;
        this.projectResolver = projectResolver;
    }

    public boolean isAllowedToAccessPath(@Nullable User user, @Nonnull Path unsafePath) {
        // anonymous is not allowed to do anything
        if (user == null) {
            return false;
        }

        // there are only two allowed scenarios
        //  - resources     => "free for all"
        //  - workspace     => depends on ACL of project

        var resourcesPath = resourceManager.getSubResourcesPath(unsafePath);
        var workspacePath = resourceManager.getSubWorkspacePath(unsafePath);

        if (resourcesPath.isPresent()) {
            return true;
        } else if (workspacePath.isPresent()) {
            // root is allowed to do everything anywhere, no further check required
            var workspace = workspacePath.get();
            return user.hasSuperPrivileges() ||
                    (workspace.getNameCount() > 0 &&
                            projectResolver
                                    .apply(workspace.getName(0).toString()) // the work directory name is the project id
                                    .map(project -> project.getOwner().equals(user.getName())
                                            || project.getGroups().stream().anyMatch(user::canAccessGroup)
                                            || project.isPublic()
                                    )
                                    .orElse(Boolean.FALSE));
        } else {
            return false;
        }
    }
}
