package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class Image {
    private @Nonnull  String   name;
    private @Nonnull  String[] args;
    private @Nullable Long     shmSizeMegabytes;

    public Image(@Nonnull String name) {
        this(name, new String[0]);
    }

    public Image(@Nonnull String name, @Nonnull String[] args) {
        this(name, args, 0L);
    }

    @ConstructorProperties({"name, args, shmSizeMegabytes"})
    public Image(@Nonnull String name, @Nullable String[] args, @Nullable Long shmSizeMegabytes) {
        Objects.requireNonNull(name, "The name of an image must be set");
        this.name = name;
        this.args = args != null ? args : new String[0];
        this.setShmSizeMegabytes(shmSizeMegabytes);
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
        return args;
    }

    public void setArgs(@Nonnull String[] args) {
        this.args = args;
    }


    /**
     * If shared memory is required, the value will be greater than zero.
     */
    @Nonnull
    public Optional<Long> getShmSizeMegabytes() {
        return Optional.ofNullable(this.shmSizeMegabytes);
    }


    /**
     * @param megabytes If shared memory is required, the value shall be greater than zero.
     */
    public void setShmSizeMegabytes(@Nullable Long megabytes) {
        this.shmSizeMegabytes = megabytes != null && megabytes > 0 ? megabytes : null;
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
