package de.itdesigners.winslow.api.file;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public record FileInfo(
        @Nonnull String name,
        boolean directory,
        @Nonnull String path,
        @Nullable Long fileSize,
        @Nonnull Map<String, Object> attributes) {
}
