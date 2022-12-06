package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.pipeline.HighlightInfo;
import de.itdesigners.winslow.config.Highlight;

public class HighlightInfoConverter {

    public static HighlightInfo from (Highlight highlight) {
        return new HighlightInfo(highlight.resources());
    }
}
