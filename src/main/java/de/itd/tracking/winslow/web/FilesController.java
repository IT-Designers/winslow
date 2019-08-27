package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Winslow;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
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

    @PutMapping(value = {"/files/resources/**"})
    public boolean createDirectory(HttpServletRequest request) {
        return normalizedPath(request)
                .flatMap(path -> winslow
                        .getResourceManager()
                        .getResourceDirectory()
                        .map(dir -> dir.resolve(path))
                )
                .map(path -> path.toFile().exists() || path.toFile().mkdirs())
                .orElse(false);
    }

    @PostMapping(value = {"/files/resources/**"})
    public boolean uploadFile(HttpServletRequest request, @RequestParam("file")MultipartFile file) {
        return normalizedPath(request)
                .flatMap(path -> winslow
                        .getResourceManager()
                        .getResourceDirectory()
                        .map(dir -> dir.resolve(path))
                )
                .map(path -> {
                    var parent = path.getParent();
                    System.out.println(path);
                    if (parent.toFile().exists() && parent.toFile().isDirectory()) {
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
                .orElse(false);
    }

    @GetMapping(value = {"/files/resources/**"})
    public Iterable<FileInfo> getResourceInfo(HttpServletRequest request) {
        return normalizedPath(request).flatMap(path -> {
            var resourceDir = winslow.getResourceManager().getResourceDirectory();
            return resourceDir.map(resDir ->
                    Optional.ofNullable(resDir.resolve(path.normalize()).toFile().listFiles())
                            .stream()
                            .flatMap(Arrays::stream)
                            .map(file -> new FileInfo(
                                    file.getName(),
                                    file.isDirectory(),
                                    Path.of("/", "resources").resolve(resDir.relativize(file.toPath())).toString()
                            ))
                            .collect(Collectors.toUnmodifiableList())
            );
        }).orElse(Collections.emptyList());
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
