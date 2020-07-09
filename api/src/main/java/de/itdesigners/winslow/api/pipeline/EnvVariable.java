package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnvVariable {

    public final @Nonnull String key;
    public @Nullable      String value;
    public @Nullable      String valueInherited;

    public EnvVariable(@Nonnull String key, @Nullable String value) {
        this.key            = key;
        this.value          = value;
        this.valueInherited = value;
    }

    public EnvVariable(@Nonnull String key) {
        this.key            = key;
        this.value          = null;
        this.valueInherited = null;
    }

    public void pushValue(@Nullable String value) {
        if (this.value != null) {
            this.valueInherited = this.value;
        }
        this.value = value;
    }
}
