package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.HighlightInfo;
import de.itdesigners.winslow.config.Highlight;

import javax.annotation.Nonnull;

public class HighlightInfoConverter {

    public static HighlightInfo from(@Nonnull Highlight highlight) {
        return new HighlightInfo(highlight.resources());
    }
}
