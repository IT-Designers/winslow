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
}
