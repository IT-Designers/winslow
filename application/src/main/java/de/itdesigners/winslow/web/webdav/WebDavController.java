package de.itdesigners.winslow.web.webdav;

import de.itdesigners.winslow.Orchestrator;
import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.web.FileAccessChecker;
import io.milton.annotations.*;
import io.milton.http.Auth;
import io.milton.http.Request;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ResourceController
public class WebDavController {

    public static final String EXPORT_NAME = "webdav";

    private @Nullable Winslow           winslow;
    private @Nullable FileAccessChecker checker;

    @Nonnull
    public Winslow getWinslow() {
        if (this.winslow == null) {
            this.winslow = MiltonConfiguration.getWinslow().orElseThrow();
        }
        return this.winslow;
    }

    @Nonnull
    public FileAccessChecker getChecker() {
        if (this.checker == null) {
            this.checker = new FileAccessChecker(
                    getWinslow().getResourceManager(),
                    id -> getWinslow().getProjectRepository().getProject(id).unsafe()
            );
        }
        return this.checker;
    }

    private boolean isAllowedToAccess(@Nonnull User user, @Nonnull WebDavEntry entry) {
        return getChecker().isAllowedToAccessPath(user, entry.getPath());
    }

    private boolean isAllowedToAccess(@Nonnull Request request, @Nonnull WebDavEntry entry) {
        return getUser(request)
                .map(user -> isAllowedToAccess(user, entry))
                .orElse(Boolean.FALSE);
    }

    @Nonnull
    private static Optional<User> getUser(@Nonnull Request request) {
        return getUser(request.getAuthorization());
    }

    @Nonnull
    private static Optional<User> getUser(@Nonnull Auth auth) {
        return MiltonConfiguration
                .getWinslow()
                .map(Winslow::getUserRepository)
                .flatMap(u -> u.getUserOrCreateAuthenticated(auth.getUser()));
    }


    @Root
    public WebDavController getRoot() {
        return this;
    }

    @ChildrenOf
    public List<WebDavRootFolder> getWebDavRoot(WebDavController root) {
        return MiltonConfiguration
                .getWinslow()
                .map(w -> new WebDavRootFolder(w, EXPORT_NAME))
                .stream()
                .collect(Collectors.toList());
    }

    @ChildOf
    public WebDavRootFolder getWebDavRoot(WebDavController root, String name) {
        return EXPORT_NAME.equals(name)
               ? new WebDavRootFolder(getWinslow(), EXPORT_NAME)
               : null;
    }

    @ChildrenOf
    public Collection<WebDavEntry> getWebDavFolders(WebDavRootFolder webDavFolder, Request req) {
        return webDavFolder
                .listFolders()
                .stream()
                .filter(folder -> isAllowedToAccess(req, folder))
                .collect(Collectors.toList());
    }

    @ChildOf
    public WebDavEntry getWebDavFolders(WebDavRootFolder webDavFolder, @Nonnull String name, Request req) {
        return webDavFolder
                .getFolder(name)
                .filter(folder -> isAllowedToAccess(req, folder))
                .orElse(null);
    }

    @ChildrenOf
    public Collection<WebDavDirectory> getWebDavFolders(WebDavDirectory webDavDirectory, Request req) {
        return webDavDirectory
                .listFolders()
                .stream()
                .filter(folder -> isAllowedToAccess(req, folder))
                .collect(Collectors.toList());
    }

    @ChildrenOf
    public Collection<WebDavFile> getWebDavFiles(WebDavDirectory webDavDirectory, Request req) {
        return webDavDirectory
                .listFiles()
                .stream()
                .filter(folder -> isAllowedToAccess(req, folder))
                .collect(Collectors.toList());
    }

    @ChildOf
    public WebDavEntry getWebDavDirectoryEntry(WebDavDirectory webDavDirectory, String name, Request req) {
        return Optional
                .of(webDavDirectory.getPath().resolve(name))
                .filter(Files::exists)
                .map(path -> Files.isDirectory(path)
                             ? new WebDavDirectory(webDavDirectory.getRoot(), path)
                             : new WebDavFile(webDavDirectory.getRoot(), path))
                .filter(entry -> isAllowedToAccess(req, entry))
                .orElse(null);
    }

    @Get
    public InputStream geInputStream(WebDavFile webDavFile) throws FileNotFoundException {
        return webDavFile.getInputStream();
    }


    @PutChild
    public WebDavFile createFile(WebDavDirectory parent, String name, InputStream inputStream, Request request) throws IOException {
        if (inputStream != null) {
            System.out.println("creating file: " + parent.getFullPath() + "/" + name);
            var file = parent.createFile(name);
            if (isAllowedToAccess(request, file)) {
                return updateFileContent(file, inputStream, request);
            } else  {
                return null;
            }
        } else {
            return null;
        }
    }

    @PutChild
    public WebDavFile createFile(WebDavDirectory parent, String name, byte[] bytes, Request request) throws IOException {
        if (bytes != null) {
            System.out.println("creating file: " + parent.getFullPath() + "/" + name);
            var file = parent.createFile(name);
            if (isAllowedToAccess(request, file)) {
                return updateFileContent(file, bytes, request);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }


    @PutChild
    public WebDavFile updateFileContent(WebDavFile file, byte[] bytes, Request request) throws IOException {
        System.out.println("updating file: " + file.getFullPath());
        if (isAllowedToAccess(request, file)) {
            try (var fos = file.getOutputStream()) {
                fos.write(bytes);
                fos.flush();
            }
            return file;
        } else {
            return null;
        }
    }

    @PutChild
    public WebDavFile updateFileContent(WebDavFile file, InputStream inputStream, Request request) throws IOException {
        System.out.println("updating file: " + file.getFullPath());
        if (isAllowedToAccess(request, file)) {
            try (var fos = file.getOutputStream()) {
                inputStream.transferTo(fos);
                fos.flush();
            }
            return file;
        } else {
            return null;
        }
    }

    @MakeCollection
    public WebDavDirectory createDirectory(@Nonnull WebDavDirectory parent, @Nonnull String name, Request request) throws IOException {
        System.out.println("creating directory: " + parent.getFullPath() + "/" + name);
        if (isAllowedToAccess(request, parent)) {
            var directory = parent.createDirectory(name);
            Files.createDirectories(directory.getFullPath());
            return directory;
        } else {
            return null;
        }
    }

    @Delete
    public void delete(WebDavFile file, Request request) throws IOException {
        System.out.println("deleting file: " + file.getFullPath());
        if (isAllowedToAccess(request, file)) {
            Files.delete(file.getFullPath());
        }
    }

    @Delete
    public void delete(WebDavDirectory directory, Request request) throws IOException {
        System.out.println("deleting directory: " + directory.getFullPath());
        if (isAllowedToAccess(request, directory)) {
            Orchestrator.forcePurge(
                    directory.getRoot(),
                    directory.getFullPath(),
                    directory.getFullPath()
            );
        }
    }

}
