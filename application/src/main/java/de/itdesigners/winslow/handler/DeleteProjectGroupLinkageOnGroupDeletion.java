package de.itdesigners.winslow.handler;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.auth.ChangeEvent;
import de.itdesigners.winslow.auth.Group;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public record DeleteProjectGroupLinkageOnGroupDeletion(@Nonnull Winslow winslow) implements ChangeEvent.Listener<Group> {

    private static final Logger LOG = Logger.getLogger(DeleteProjectGroupLinkageOnGroupDeletion.class.getSimpleName());

    @Override
    public void onEvent(@Nonnull ChangeEvent<Group> event) {
        winslow
                .getProjectRepository()
                .getProjects()
                .filter(handle -> handle.unsafe().map(p -> p
                                .getGroups()
                                .stream()
                                .anyMatch(g -> event.value().name().equals(g.name()))
                        ).orElse(Boolean.FALSE)
                )
                .forEach(handle -> {
                    handle.exclusive().ifPresent(container -> {
                        try (container) {
                            var project = container.getNoThrow().orElseThrow(IOException::new);
                            project.removeGroup(event.value().name());
                            container.update(project);
                        } catch (IOException e) {
                            LOG.log(Level.SEVERE, "Failed to delete group linkage from project", e);
                        }
                    });
                });
    }
}
