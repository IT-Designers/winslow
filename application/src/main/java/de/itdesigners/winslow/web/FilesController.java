package de.itdesigners.winslow.web;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.resource.ResourceManager;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class FilesController {

    private final ResourceManager   resourceManager;
    private final FileAccessChecker checker;

    @Autowired
    public FilesController(Winslow winslow) {
        this(
                winslow.getResourceManager(),
                new FileAccessChecker(
                        winslow.getResourceManager(),
                        id -> winslow.getProjectRepository().getProject(id).unsafe()
                )
        );
    }

    public FilesController(@Nonnull ResourceManager resourceManager, @Nonnull FileAccessChecker checker) {
        this.resourceManager = resourceManager;
        this.checker         = checker;
    }

    @DeleteMapping(value = {"/files/resources/**"})
    public boolean deleteInResource(HttpServletRequest request, User user) {
        return resourceManager
                .getResourceDirectory()
                .map(dir -> delete(request, user, dir, false))
                .orElse(Boolean.FALSE);
    }

    @DeleteMapping(value = {"/files/workspaces/**"})
    public boolean deleteInWorkspace(HttpServletRequest request, User user) {
        return resourceManager
                .getWorkspacesDirectory()
                .map(dir -> delete(request, user, dir, true))
                .orElse(Boolean.FALSE);
    }

    public boolean delete(HttpServletRequest request, User user, @Nonnull Path directory, boolean protectTopLevel) {
        return normalizedPath(request)
                .flatMap(path -> Optional
                        .of(directory.resolve(path))
                        .filter(p -> !protectTopLevel || path.getNameCount() > 1) // prevent deletion of top level directories
                        .filter(p -> checker.isAllowedToAccessPath(user, p)))
                .map(path -> {
                    try {
                        FileUtils.forceDelete(path.toFile());
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                }).orElse(false);
    }


    @PutMapping(value = {"/files/resources/**"})
    public Optional<String> createResourceDirectory(HttpServletRequest request, User user) {
        return resourceManager
                .getResourceDirectory()
                .flatMap(dir -> createDirectory(request, user, dir)
                        .map(dir::relativize)
                        .map(Path.of("/resources/")::resolve)
                )
                .map(Path::toString);
    }

    @PutMapping(value = {"/files/workspaces/**"})
    public Optional<String> createWorkspaceDirectory(HttpServletRequest request, User user) {
        return resourceManager
                .getWorkspacesDirectory()
                .flatMap(dir -> createDirectory(request, user, dir)
                        .map(dir::relativize)
                        .map(Path.of("/workspaces/")::resolve)
                )
                .map(Path::toString);
    }

    public Optional<Path> createDirectory(HttpServletRequest request, User user, @Nonnull Path directory) {
        return normalizedPath(request)
                .flatMap(path -> Optional
                        .of(directory.resolve(path))
                        .filter(p -> checker.isAllowedToAccessPath(user, p))
                        .filter(p -> p.toFile().exists() || p.toFile().mkdirs())
                );
    }

    @PostMapping(value = {"/files/resources/**"})
    public void uploadResourceFile(HttpServletRequest request, User user, @RequestParam("file") MultipartFile file) {
        uploadFile(request, user, file, resourceManager.getResourceDirectory().orElseThrow());
    }

    @PostMapping(value = {"/files/workspaces/**"})
    public void uploadWorkspaceFile(HttpServletRequest request, User user, @RequestParam("file") MultipartFile file) {
        uploadFile(request, user, file, resourceManager.getWorkspacesDirectory().orElseThrow());
    }

    public void uploadFile(
            HttpServletRequest request,
            User user,
            @RequestParam("file") MultipartFile file,
            @Nonnull Path directory) {
        normalizedPath(request)
                .flatMap(path -> Optional
                        .of(directory.resolve(path))
                        .filter(p -> checker.isAllowedToAccessPath(user, p)))
                .map(path -> {
                    var parent = path.getParent();
                    if ((parent.toFile().exists() || parent.toFile().mkdirs()) && parent.toFile().isDirectory()) {
                        try {
                            file.transferTo(path);
                            return true;
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    } else {
                        return false;
                    }
                })
                .flatMap(result -> result ? Optional.of(true) : Optional.empty())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(value = {"/files/resources/**"}, method = RequestMethod.GET)
    public ResponseEntity<? extends Resource> downloadResourceFile(HttpServletRequest request, User user) {
        return downloadFile(request, user, resourceManager.getResourceDirectory().orElseThrow());
    }

    @RequestMapping(value = {"/files/workspaces/**"}, method = RequestMethod.GET)
    public ResponseEntity<? extends Resource> downloadWorkspaceFile(HttpServletRequest request, User user) {
        return downloadFile(request, user, resourceManager.getWorkspacesDirectory().orElseThrow());
    }

    public ResponseEntity<? extends Resource> downloadFile(
            HttpServletRequest request,
            User user,
            @Nonnull Path directory) {
        return normalizedPath(request)
                .map(directory::resolve)
                .filter(path -> checker.isAllowedToAccessPath(user, path))
                .filter(Files::exists)
                .map(Path::toFile)
                .map(file -> ResponseEntity
                        .ok()
                        // Otherwise the download might result in a "f.txt" named file
                        // https://stackoverflow.com/questions/41364732/zip-file-downloaded-as-f-txt-file-springboot
                        // https://pivotal.io/security/cve-2015-5211
                        .header(
                                "content-disposition",
                                "inline; filename=\"" + file.getName().replaceAll("\"", "") + "\""
                        )
                        .contentLength(file.length())
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(new FileSystemResource(file))
                )
                .orElse(null);
    }

    @RequestMapping(value = {"/files/resources/**"}, method = RequestMethod.OPTIONS)
    public Iterable<FileInfo> listResourceDirectory(HttpServletRequest request, User user) {
        return listDirectory(
                request,
                user,
                resourceManager.getResourceDirectory().orElseThrow(),
                Path.of("/resources/")
        );
    }

    @RequestMapping(value = {"/files/workspaces/**"}, method = RequestMethod.OPTIONS)
    public Iterable<FileInfo> listWorkspaceDirectory(HttpServletRequest request, User user) {
        return listDirectory(
                request,
                user,
                resourceManager.getWorkspacesDirectory().orElseThrow(),
                Path.of("/workspaces/")
        );
    }

    private Iterable<FileInfo> listDirectory(
            HttpServletRequest request,
            User user,
            @Nonnull Path directory,
            @Nonnull Path resolveTo) {
        return normalizedPath(request)
                .map(path -> Optional
                        .of(directory.resolve(path))
                        .filter(p -> checker.isAllowedToAccessPath(user, p))
                        .flatMap(p -> Optional.ofNullable(p.toFile().listFiles()))
                        .stream()
                        .flatMap(Arrays::stream)
                        .map(file -> new FileInfo(
                                     file.getName(),
                                     file.isDirectory(),
                                     resolveTo.resolve(directory.relativize(file.toPath())).toString(),
                                     Optional.of(file)
                                             .filter(File::isFile)
                                             .map(File::length)
                                             .orElse(null)
                             )
                        )
                        .collect(Collectors.toUnmodifiableList())
                )
                .orElse(Collections.emptyList());
    }

    @Nonnull
    protected static Optional<Path> normalizedPath(HttpServletRequest request) {
        var pathWithinHandler = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        var bestMatch         = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return normalizedPath(bestMatch, pathWithinHandler);
    }

    @Nonnull
    private static Optional<Path> normalizedPath(@Nonnull String pattern, @Nonnull String rawPath) {
        var extracted = new AntPathMatcher().extractPathWithinPattern(pattern, rawPath);
        var path      = Path.of(extracted).normalize();

        // for se security
        if (path.isAbsolute()) {
            return Optional.empty();
        } else {
            return Optional.of(path);
        }
    }

    public static class FileInfo {
        public final @Nonnull  String  name;
        public final           boolean directory;
        public final @Nonnull  String  path;
        public final @Nullable Long    fileSize;

        public FileInfo(String name, boolean isDirectory, String path, @Nullable Long fileSize) {
            this.name      = name;
            this.directory = isDirectory;
            this.path      = path;
            this.fileSize  = fileSize;
        }
    }
}
