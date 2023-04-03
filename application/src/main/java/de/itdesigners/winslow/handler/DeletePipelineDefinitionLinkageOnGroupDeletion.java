package de.itdesigners.winslow.handler;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.auth.ChangeEvent;
import de.itdesigners.winslow.auth.Group;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public record DeletePipelineDefinitionLinkageOnGroupDeletion(
        @Nonnull Winslow winslow) implements ChangeEvent.Listener<Group> {


    private static final Logger LOG = Logger.getLogger(DeletePipelineDefinitionLinkageOnGroupDeletion.class.getSimpleName());

    @Override
    public void onEvent(@Nonnull ChangeEvent<Group> event) {
        var groupName = event.value().name();
        winslow
                .getPipelineRepository()
                .getPipelines()
                .filter(handle -> handle.unsafe().map(
                        pd -> pd.groups().stream().anyMatch(l -> Objects.equals(l.name(), groupName))
                ).orElse(Boolean.FALSE))
                .forEach(handle -> {
                    handle.exclusive().ifPresent(container -> {
                        try (container) {
                            container.update(
                                    container
                                            .getNoThrow()
                                            .orElseThrow(IOException::new)
                                            .withoutGroup(groupName)
                            );
                        } catch (IOException e) {
                            LOG.log(
                                    Level.SEVERE,
                                    "Failed to delete group linkage from pipeline",
                                    e
                            );
                        }
                    });
                });
    }
}
