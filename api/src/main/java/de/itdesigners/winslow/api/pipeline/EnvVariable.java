package de.itdesigners.winslow.api.pipeline;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.beans.Transient;

public record EnvVariable(
        @Nonnull String key,
        @Nullable String value,
        @Nullable String valueInherited) {

    public EnvVariable(@Nonnull String key) {
        this(key, null);
    }


    public EnvVariable(@Nonnull String key, @Nullable String value) {
        this(key, value, null);
    }

    @ConstructorProperties({"key", "value", "valueInherited"})
    public EnvVariable {
    }

    @Nonnull
    @Transient
    @CheckReturnValue
    public EnvVariable pushValue(@Nullable String value) {
        return new EnvVariable(
                key,
                value,
                this.value
        );
    }
}
