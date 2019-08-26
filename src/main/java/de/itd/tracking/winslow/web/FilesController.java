package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Winslow;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class FilesController {

    private final Winslow winslow;

    public FilesController(Winslow winslow) {
        this.winslow = winslow;
    }

    @PutMapping(value = {"/files/resources/**"})
    public void createDirectory(HttpServletRequest request) {
        normalizedPath(request).ifPresent(path -> {
            System.out.println(path);
        });
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
                                    Path.of("resources").resolve(resDir.relativize(file.toPath())).toString()
                            ))
                            .collect(Collectors.toUnmodifiableList())
            );
        }).orElse(Collections.emptyList());
    }

    private static Optional<Path> normalizedPath(HttpServletRequest request) {
        var  pathWithinHandler = (String)request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        var bestMatch = (String)request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        var path = Path.of(new AntPathMatcher().extractPathWithinPattern(bestMatch, pathWithinHandler)).normalize();

        System.out.println(path);

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
