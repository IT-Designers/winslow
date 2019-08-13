package de.itd.tracking.winslow.config;

import java.util.Arrays;
import java.util.Objects;

public class Image {
    private final String   name;
    private final String[] args;

    public Image(String name, String[] args) {
        Objects.requireNonNull(name, "The name of an image must be set");
        this.name = name;
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public String[] getArguments() {
        return args != null ? args : new String[0];
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"@{name='"+this.name+"',args="+ Arrays.toString(this.args) +"}#"+this.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Image image = (Image) o;
        return Objects.equals(name, image.name) && Arrays.equals(args, image.args);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name);
        result = 31 * result + Arrays.hashCode(args);
        return result;
    }
}
