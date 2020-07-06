package de.itdesigners.winslow.api.file;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class FileInfo {

    public final @Nonnull  String              name;
    public final           boolean             directory;
    public final @Nonnull  String              path;
    public final @Nullable Long                fileSize;
    public final @Nonnull  Map<String, Object> attributes;

    public FileInfo(
            @Nonnull String name,
            boolean isDirectory,
            @Nonnull String path,
            @Nullable Long fileSize,
            @Nonnull Map<String, Object> attributes) {
        this.name      = name;
        this.directory = isDirectory;
        this.path      = path;
        this.fileSize  = fileSize;
        this.attributes = attributes;
    }
}
