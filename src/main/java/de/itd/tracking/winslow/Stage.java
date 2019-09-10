package de.itd.tracking.winslow;

import de.itd.tracking.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;

public interface Stage {

    enum State {
        Running,
        Succeeded,
        Failed
    }

    @Nonnull
    StageDefinition getDefinition();

    @Nonnull
    Date getStartTime();

    @Nullable
    Date getFinishTime();

    @Nonnull
    State getState();
}
