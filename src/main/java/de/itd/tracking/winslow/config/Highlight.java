package de.itd.tracking.winslow.config;

import java.util.Arrays;
import java.util.Collections;

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
}
