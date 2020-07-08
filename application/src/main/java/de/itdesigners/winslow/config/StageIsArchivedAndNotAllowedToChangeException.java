package de.itdesigners.winslow.config;

import de.itdesigners.winslow.pipeline.Stage;

import javax.annotation.Nonnull;

public class StageIsArchivedAndNotAllowedToChangeException extends Throwable {

    private final @Nonnull ExecutionGroup group;
    private final @Nonnull Stage          stage;

    public StageIsArchivedAndNotAllowedToChangeException(@Nonnull ExecutionGroup goup, @Nonnull Stage stage) {
        super("The stage with the id " + stage.getId() + " is archived and therefore not allowed to change anymore");
        this.group = goup;
        this.stage = stage;
    }

    /**
     * @return The {@link ExecutionGroup} that denied the update
     */
    @Nonnull
    public ExecutionGroup getExecutionGroup() {
        return group;
    }

    /**
     * @return The archived stage
     */
    @Nonnull
    public Stage getStage() {
        return stage;
    }
}
