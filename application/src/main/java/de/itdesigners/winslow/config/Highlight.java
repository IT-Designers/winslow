package de.itdesigners.winslow.config;

import java.util.Arrays;

public class Highlight {
    private final String[] resources;

    public Highlight(String[] resources) {
        this.resources = resources;
    }

    public String[] getResources() {
        return resources != null ? resources : new String[0];
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{resources=" + Arrays.toString(this.resources) + "}#" + this.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Highlight && Arrays.deepEquals(((Highlight) obj).resources, resources);
    }
}
