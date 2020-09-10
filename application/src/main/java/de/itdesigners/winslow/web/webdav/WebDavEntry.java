package de.itdesigners.winslow.web.webdav;

import io.milton.annotations.CreatedDate;
import io.milton.annotations.Name;
import io.milton.annotations.UniqueId;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Date;

public abstract class WebDavEntry {

    protected final @Nonnull Path root;
    protected final @Nonnull Path path;
    protected final @Nonnull String name;

    public WebDavEntry(@Nonnull Path root, @Nonnull Path path) {
        this(root, path, path.getFileName().toString());
    }

    public WebDavEntry(@Nonnull Path root, @Nonnull Path path, @Nonnull String name) {
        this.root = root;
        this.path = path;
        this.name = name;
    }

    @Nonnull
    public Path getRoot() {
        return root;
    }

    @Nonnull
    public Path getPath() {
        return path;
    }

    @Nonnull
    public Path getFullPath() {
        return root.resolve(path);
    }

    @Name
    @Nonnull
    public String getName() {
        return name;
    }

    @UniqueId
    public String getUniqueId() {
        return path.toString();
    }

    @CreatedDate
    public Date getCreatedDate() {
        return new Date(root.resolve(path).toFile().lastModified());
    }
}
