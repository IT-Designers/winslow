package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class Image {
    private @Nonnull String   name;
    private @Nonnull String[] args;
    private @Nonnull Integer  shmSizeMegabytes;

    public Image(@Nonnull String name, @Nonnull String[] args) {
        this(name, args, null);
    }

    public Image() {
        this("", new String[0]);
    }

    @ConstructorProperties({"name, args, shmSizeMegabytes"})
    public Image(@Nonnull String name, @Nonnull String[] args, @Nullable Integer shmSizeMegabytes) {
        Objects.requireNonNull(name, "The name of an image must be set");
        this.name             = name;
        this.args             = args;
        this.shmSizeMegabytes = shmSizeMegabytes != null ? shmSizeMegabytes : 0;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public void setName(@Nonnull String name) {
        this.name = name;
    }

    @Nonnull
    public String[] getArgs() {
        return args != null ? args : new String[0];
    }

    public void setArgs(@Nullable String[] args) {
        this.args = args;
    }

    @Nonnull
    public Integer getShmSizeMegabytes() {
        return this.shmSizeMegabytes;
    }

    public void setShmSizeMegabytes(@Nullable Integer megabytes) {
        this.shmSizeMegabytes = megabytes;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{name='" + this.name + "',args=" + Arrays.toString(this.args) + "}#" + this
                .hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Image image = (Image) o;
        return Objects.equals(name, image.name)
                && Arrays.equals(args, image.args)
                && Objects.equals(shmSizeMegabytes, image.shmSizeMegabytes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name, shmSizeMegabytes);
        result = 31 * result + Arrays.hashCode(args);
        return result;
    }
}
