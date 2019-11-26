package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Winslow;
import de.itd.tracking.winslow.auth.User;
import org.apache.tomcat.util.http.fileupload.FileUtils;
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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
public class FilesController {

    private static final Logger LOG = Logger.getLogger(FilesController.class.getSimpleName());

    private final Winslow winslow;

    public FilesController(Winslow winslow) {
        this.winslow = winslow;
    }

    @DeleteMapping(value = {"/files/resources/**"})
    public boolean deleteInResource(HttpServletRequest request, User user) {
        return delete(request, user, winslow.getResourceManager().getResourceDirectory());
    }

    @DeleteMapping(value = {"/files/workspaces/**"})
    public boolean deleteInWorkspace(HttpServletRequest request, User user) {
        return delete(request, user, winslow.getResourceManager().getWorkspacesDirectory());
    }

    public boolean delete(HttpServletRequest request, User user, @Nonnull Optional<Path> directory) {
        return normalizedPath(request).flatMap(path -> directory.flatMap(dir -> {
            // prevent deletion of '/resources/'
            return Optional
                    .of(dir.resolve(path))
                    .filter(resolved -> !resolved.equals(dir))
                    .filter(resolved -> canAccess(winslow, user, dir, resolved));
        })).map(path -> {
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
        return createDirectory(request, user, winslow.getResourceManager().getResourceDirectory());
    }

    @PutMapping(value = {"/files/workspaces/**"})
    public Optional<String> createWorkspaceDirectory(HttpServletRequest request, User user) {
        return createDirectory(request, user, winslow.getResourceManager().getWorkspacesDirectory());
    }

    public Optional<String> createDirectory(HttpServletRequest request, User user, @Nonnull Optional<Path> directory) {
        return normalizedPath(request).flatMap(path -> directory.flatMap(dir -> Optional
                .of(dir.resolve(path))
                .filter(p -> canAccessOrCreateDirectory(winslow, user, dir, p, true))
                .filter(p -> p.toFile().exists() || p.toFile().mkdirs())
                .map(p -> Path.of("/resources/").resolve(path).toString())));
    }

    @PostMapping(value = {"/files/resources/**"})
    public void uploadResourceFile(HttpServletRequest request, User user, @RequestParam("file") MultipartFile file) {
        uploadFile(request, user, file, winslow.getResourceManager().getResourceDirectory());
    }

    @PostMapping(value = {"/files/workspaces/**"})
    public void uploadWorkspaceFile(HttpServletRequest request, User user, @RequestParam("file") MultipartFile file) {
        uploadFile(request, user, file, winslow.getResourceManager().getWorkspacesDirectory());
    }

    public void uploadFile(
            HttpServletRequest request,
            User user,
            @RequestParam("file") MultipartFile file,
            @Nonnull Optional<Path> directory) {
        normalizedPath(request)
                .flatMap(path -> directory.flatMap(dir -> {
                    var resolved = dir.resolve(path);
                    if (canAccess(winslow, user, dir, resolved)) {
                        return Optional.of(resolved);
                    } else {
                        return Optional.empty();
                    }
                }))
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
        return downloadFile(request, user, winslow.getResourceManager().getResourceDirectory());
    }

    @RequestMapping(value = {"/files/workspaces/**"}, method = RequestMethod.GET)
    public ResponseEntity<? extends Resource> downloadWorkspaceFile(HttpServletRequest request, User user) {
        return downloadFile(request, user, winslow.getResourceManager().getWorkspacesDirectory());
    }

    public ResponseEntity<? extends Resource> downloadFile(
            HttpServletRequest request, User user, @Nonnull Optional<Path> directory) {
        return normalizedPath(request).flatMap(path -> directory.flatMap(dir -> {
            var file = dir.resolve(path.normalize()).toFile();

            if (!canAccess(winslow, user, dir, file.toPath())) {
                return Optional.empty();
            }

            return Optional.of(
                    ResponseEntity
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
            );
        })).orElse(null);
    }

    @RequestMapping(value = {"/files/resources/**"}, method = RequestMethod.OPTIONS)
    public Iterable<FileInfo> listResourceDirectory(HttpServletRequest request, User user) {
        return listDirectory(request, user, winslow.getResourceManager().getResourceDirectory());
    }

    @RequestMapping(value = {"/files/workspaces/**"}, method = RequestMethod.OPTIONS)
    public Iterable<FileInfo> listWorkspaceDirectory(HttpServletRequest request, User user) {
        return listDirectory(request, user, winslow.getResourceManager().getWorkspacesDirectory());
    }

    public Iterable<FileInfo> listDirectory(HttpServletRequest request, User user, @Nonnull Optional<Path> directory) {
        return normalizedPath(request).flatMap(path -> directory.map(dir -> Optional
                .ofNullable(dir
                                    .resolve(path.normalize())
                                    .toFile()
                                    .listFiles())
                .stream()
                .flatMap(Arrays::stream)
                .filter(file -> canAccess(winslow, user, dir, file.toPath()))
                .map(file -> new FileInfo(file.getName(), file.isDirectory(), Path
                        .of(
                                "/",
                                dir
                                        .getName(dir.getNameCount() - 1)
                                        .toString()
                        )
                        .resolve(dir.relativize(file.toPath()))
                        .toString(),
                                          Optional.of(file)
                                                  .filter(f -> !f.isDirectory())
                                                  .map(File::length)
                                                  .orElse(null)

                ))
                .collect(Collectors.toUnmodifiableList()))).orElse(Collections.emptyList());
    }

    private static boolean canAccess(@Nonnull Winslow winslow, @Nullable User user, Path workDir, Path path) {
        return canAccessOrCreateDirectory(winslow, user, workDir, path, false);
    }

    private static boolean canAccessOrCreateDirectory(
            @Nonnull Winslow winslow,
            @Nullable User user,
            @Nonnull Path workDir,
            @Nonnull Path path,
            boolean wantsCreateDirectory) {
        if (user != null && user.hasSuperPrivileges()) {
            return true;
        }

        var p = workDir.relativize(path);

        //  TODO
        if (workDir.endsWith("workspaces") && p.getNameCount() > 0) {
            return winslow
                    .getProjectRepository()
                    .getProject(p.getName(0).toString())
                    .unsafe()
                    .filter(project -> user != null && (
                            user.getName().equals(project.getOwner())
                                    || user.getGroups().anyMatch(user::canAccessGroup)))
                    .isPresent();
        }

        return user != null
                && p.getNameCount() > 0
                && (workDir.resolve(p.getName(0)).toFile().exists() || wantsCreateDirectory)
                && (workDir.resolve(p.getName(0)).toFile().isFile() || user.canAccessGroup(p.getName(0).toString()));
    }

    private static Optional<Path> normalizedPath(HttpServletRequest request) {
        var pathWithinHandler = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        var bestMatch         = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        var path = Path
                .of(new AntPathMatcher().extractPathWithinPattern(bestMatch, pathWithinHandler))
                .normalize();

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
