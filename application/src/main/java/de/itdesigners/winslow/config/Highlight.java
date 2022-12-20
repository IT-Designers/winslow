package de.itdesigners.winslow.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public record Highlight(@Nonnull List<String> resources) {

    public Highlight() {
        this(null);
    }

    public Highlight(@Nullable List<String> resources) {
        this.resources = resources != null ? resources : Collections.emptyList();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{resources=" + (this.resources()) + "}#" + this.hashCode();
    }

}
