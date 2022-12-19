package de.itdesigners.winslow.api.pipeline;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

public record HighlightInfo(
        @Nonnull List<String> resources) {

    public HighlightInfo() {
        this(Collections.emptyList());
    }

}
