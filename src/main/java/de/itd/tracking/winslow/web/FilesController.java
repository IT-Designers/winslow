package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Winslow;
import de.itd.tracking.winslow.auth.User;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class FilesController {

    private final Winslow winslow;

    public FilesController(Winslow winslow) {
        this.winslow = winslow;
    }

    @DeleteMapping(value = {"/files/resources/**"})
    public boolean delete(HttpServletRequest request, User user) {
        return normalizedPath(request)
                .flatMap(path -> winslow
                        .getResourceManager()
                        .getResourceDirectory()
                        .flatMap(dir -> {
                            // prevent deletion of '/resources/'
                            return Optional.of(dir.resolve(path))
                                    .filter(resolved -> !resolved.equals(dir))
                                    .filter(resolved -> canAccess(user, dir, resolved));
                        })
                )
                .map(path -> {
                    try {
                        FileUtils.forceDelete(path.toFile());
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .orElse(false);
    }


    @PutMapping(value = {"/files/resources/**"})
    public Optional<String> createDirectory(HttpServletRequest request, User user) {
        return normalizedPath(request)
                .flatMap(path -> winslow
                        .getResourceManager()
                        .getResourceDirectory()
                        .flatMap(dir -> Optional.of(dir.resolve(path))
                                .filter(p -> canAccessOrCreateDirectory(user, dir, p, true))
                                .filter(p -> p.toFile().exists() || p.toFile().mkdirs())
                                .map(p -> Path.of("/resources/").resolve(path).toString()))
                );
    }

    @PostMapping(value = {"/files/resources/**"})
    public void uploadFile(HttpServletRequest request, User user, @RequestParam("file")MultipartFile file) {
        normalizedPath(request)
                .flatMap(path -> winslow
                        .getResourceManager()
                        .getResourceDirectory()
                        .flatMap(dir -> {
                            var resolved = dir.resolve(path);
                            if (canAccess(user, dir, resolved)) {
                                return Optional.of(resolved);
                            } else {
                                return Optional.empty();
                            }
                        })
                )
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
                .map(result -> result ? Optional.of(true) : Optional.empty())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(value = {"/files/resources/**"}, method = RequestMethod.GET)
    public ResponseEntity<InputStreamResource> downloadFile(HttpServletRequest request, User user) {
        return normalizedPath(request).flatMap(path -> {
            var resourceDir = winslow.getResourceManager().getResourceDirectory();
            return resourceDir.flatMap(resDir -> {
                try {
                    var file = resDir.resolve(path.normalize()).toFile();

                    if (!canAccess(user, resDir, file.toPath())) {
                        return Optional.empty();
                    }

                    var       is    = new FileInputStream(file);
                    MediaType media;

                    try {
                        media = MediaType.parseMediaType(file.getName());
                    } catch (Throwable t) {
                        media = MediaType.APPLICATION_OCTET_STREAM;
                    }

                    return Optional.of(ResponseEntity.ok()
                            .contentLength(file.length())
                            .contentType(media)
                            .body(new InputStreamResource(is, file.getName())));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return Optional.empty();
                }
            });
        }).orElse(null);
    }

    @RequestMapping(value = {"/files/resources/**"}, method = RequestMethod.OPTIONS)
    public Iterable<FileInfo> listDirectory(HttpServletRequest request, User user) {
        return normalizedPath(request).flatMap(path -> {
            var resourceDir = winslow.getResourceManager().getResourceDirectory();
            return resourceDir.map(resDir ->
                    Optional.ofNullable(resDir.resolve(path.normalize()).toFile().listFiles())
                            .stream()
                            .flatMap(Arrays::stream)
                            .filter(file -> canAccess(user, resDir, file.toPath()))
                            .map(file -> new FileInfo(
                                    file.getName(),
                                    file.isDirectory(),
                                    Path.of("/", "resources").resolve(resDir.relativize(file.toPath())).toString()
                            ))
                            .collect(Collectors.toUnmodifiableList())
            );
        }).orElse(Collections.emptyList());
    }

    private static boolean canAccess(@Nullable User user, Path workDir, Path path) {
        return canAccessOrCreateDirectory(user, workDir, path, false);
    }

    private static boolean canAccessOrCreateDirectory(@Nullable User user, Path workDir, Path path, boolean wantsCreateDirectory) {
        var p = workDir.relativize(path);
        return user != null
                && p.getNameCount() > 0
                && (workDir.resolve(p.getName(0)).toFile().exists() ? workDir.resolve(p.getName(0)).toFile().isDirectory() : wantsCreateDirectory)
                && user.canAccessGroup(p.getName(0).toString());
    }

    private static Optional<Path> normalizedPath(HttpServletRequest request) {
        var  pathWithinHandler = (String)request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        var bestMatch = (String)request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        var path = Path.of(new AntPathMatcher().extractPathWithinPattern(bestMatch, pathWithinHandler)).normalize();

        // for se security
        if (path.isAbsolute()) {
            return Optional.empty();
        } else {
            return Optional.of(path);
        }
    }

    public static class FileInfo {
        private final String name;
        private final boolean isDirectory;
        private final String path;

        public FileInfo(String name, boolean isDirectory, String path) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.path = path;
        }

        public String getName() {
            return name;
        }

        public boolean isDirectory() {
            return isDirectory;
        }

        public String getPath() {
            return path;
        }
    }
}
