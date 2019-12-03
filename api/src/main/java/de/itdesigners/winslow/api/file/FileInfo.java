package de.itdesigners.winslow.api.file;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FileInfo {

    public final @Nonnull  String  name;
    public final           boolean directory;
    public final @Nonnull  String  path;
    public final @Nullable Long    fileSize;

    public FileInfo(@Nonnull String name, boolean isDirectory, @Nonnull String path, @Nullable Long fileSize) {
        this.name      = name;
        this.directory = isDirectory;
        this.path      = path;
        this.fileSize  = fileSize;
    }
}
