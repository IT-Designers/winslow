package de.itdesigners.winslow.web.webdav;

import de.itdesigners.winslow.auth.User;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

public class WebDavDirectory extends WebDavEntry {


    public WebDavDirectory(@Nonnull Path root, @Nonnull Path path) {
        super(root, path);
    }

    public WebDavDirectory(@Nonnull Path root, @Nonnull Path path, @Nonnull String name) {
        super(root, path, name);
    }

    @Override
    public Date getCreatedDate() {
        return new Date();
    }

    @Nonnull
    public WebDavFile createFile(@Nonnull String name) {
        return new WebDavFile(root, path.resolve(name));
    }

    @Nonnull
    public WebDavDirectory createDirectory(@Nonnull String name) {
        return new WebDavDirectory(root, path.resolve(name));
    }

    public Collection<WebDavFile> listFiles() {
        try (var stream = Files.list(getFullPath())) {
            return stream
                    .filter(path -> !Files.isDirectory(path))
                    .map(path -> new WebDavFile(root, root.relativize(path)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public Collection<WebDavDirectory> listFolders() {
        try (var stream = Files.list(getFullPath())) {
            return stream
                    .filter(Files::isDirectory)
                    .map(path -> new WebDavDirectory(root, root.relativize(path)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
