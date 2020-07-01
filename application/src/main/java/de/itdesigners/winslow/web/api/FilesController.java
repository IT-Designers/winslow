package de.itdesigners.winslow.web.api;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.file.FileInfo;
import de.itdesigners.winslow.auth.User;
import de.itdesigners.winslow.resource.ResourceManager;
import de.itdesigners.winslow.web.FileAccessChecker;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
    public ResponseEntity<Object> deleteInResource(HttpServletRequest request, User user) {
        var result = resourceManager
                .getResourceDirectory()
                .map(dir -> delete(request, user, dir, false))
                .orElse(Boolean.FALSE);
        if (result) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping(value = {"/files/workspaces/**"})
    public ResponseEntity<Object> deleteInWorkspace(HttpServletRequest request, User user) {
        var result = resourceManager
                .getWorkspacesDirectory()
                .map(dir -> delete(request, user, dir, true))
                .orElse(Boolean.FALSE);
        if (result) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
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
                        e.printStackTrace();
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
    public void uploadResourceFile(
            HttpServletRequest request,
            User user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "decompressArchive", defaultValue = "false", required = false) boolean decompressArchive) {
        uploadFile(request, user, file, resourceManager.getResourceDirectory().orElseThrow(), decompressArchive);
    }

    @PostMapping(value = {"/files/workspaces/**"})
    public void uploadWorkspaceFile(
            HttpServletRequest request,
            User user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "decompressArchive", defaultValue = "false", required = false) boolean decompressArchive) {
        uploadFile(request, user, file, resourceManager.getWorkspacesDirectory().orElseThrow(), decompressArchive);
    }

    public void uploadFile(
            HttpServletRequest request,
            User user,
            MultipartFile file,
            @Nonnull Path directory,
            boolean decompressArchive) {
        normalizedPath(request)
                .flatMap(path -> Optional
                        .of(directory.resolve(path))
                        .filter(p -> checker.isAllowedToAccessPath(user, p)))
                .map(path -> {
                    var parent = path.getParent();
                    if ((parent.toFile().exists() || parent.toFile().mkdirs()) && parent.toFile().isDirectory()) {
                        try {
                            if (!decompressArchive) {
                                file.transferTo(path);
                            } else {
                                decompressArchiveContentTo(file, path);
                            }
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

    private void decompressArchiveContentTo(MultipartFile file, Path path) throws IOException {
        var fileName = path.getFileName().toString();
        var target = path.getParent();

        if (fileName.endsWith(".zip")) {
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                decompressZipArchiveContentTo(zis, target);
            }
        } else if (fileName.endsWith(".tar.gz")) {
            try (GzipCompressorInputStream gcis = new GzipCompressorInputStream(file.getInputStream())) {
                try (TarArchiveInputStream tais = new TarArchiveInputStream(gcis)) {
                    decompressTarGzArchiveContentTo(tais, target);
                }
            }
        } else {
            throw new IOException("Unsupported archive format");
        }
    }

    private void decompressZipArchiveContentTo(ZipInputStream zis, Path path) throws IOException {
        var entry = (ZipEntry) null;
        while ((entry = zis.getNextEntry()) != null) {
            var file = path.resolve(entry.getName());
            if (!entry.isDirectory()) {
                if (!file.getParent().toFile().exists()) {
                    Files.createDirectories(file.getParent());
                }
                try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
                    zis.transferTo(fos);
                }
            }
        }
    }

    private void decompressTarGzArchiveContentTo(TarArchiveInputStream tais, Path path) throws IOException {
        var entry = (TarArchiveEntry) null;
        while ((entry = tais.getNextTarEntry()) != null) {
            var file = path.resolve(entry.getName());
            if (!entry.isDirectory()) {
                if (!file.getParent().toFile().exists()) {
                    Files.createDirectories(file.getParent());
                }
                try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
                    tais.transferTo(fos);
                }
            }
        }
    }

    @RequestMapping(value = {"/files/resources/**"}, method = RequestMethod.GET)
    public ResponseEntity<StreamingResponseBody> downloadResourceFile(HttpServletRequest request, User user) {
        return downloadFileOrDirectoryAsTarGz(request, user, resourceManager.getResourceDirectory().orElseThrow());
    }

    @RequestMapping(value = {"/files/workspaces/**"}, method = RequestMethod.GET)
    public ResponseEntity<StreamingResponseBody> downloadWorkspaceFile(HttpServletRequest request, User user) {
        return downloadFileOrDirectoryAsTarGz(request, user, resourceManager.getWorkspacesDirectory().orElseThrow());
    }

    public ResponseEntity<StreamingResponseBody> downloadFileOrDirectoryAsTarGz(
            HttpServletRequest request,
            User user,
            @Nonnull Path directory) {
        return normalizedPath(request)
                .map(directory::resolve)
                .filter(path -> checker.isAllowedToAccessPath(user, path))
                .filter(Files::exists)
                .map(Path::toFile)
                .map(file -> {
                    var name = (file.isFile() ? file.getName() : file.getName() + ".tar.gz").replaceAll("\"", "");
                    var responseEntity = ResponseEntity
                            .ok()
                            // Otherwise the download might result in a "f.txt" named file
                            // https://stackoverflow.com/questions/41364732/zip-file-downloaded-as-f-txt-file-springboot
                            // https://pivotal.io/security/cve-2015-5211
                            .header(
                                    HttpHeaders.CONTENT_DISPOSITION,
                                    "inline; filename=\"" + name + "\""
                            );


                    if (file.isFile()) {
                        return responseEntity
                                .contentLength(file.length())
                                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                .body((StreamingResponseBody) outputStream -> Files.copy(file.toPath(), outputStream));
                    } else {
                        return responseEntity
                                .contentType(new MediaType("application", "tar+gzip"))
                                .body((StreamingResponseBody) outputStream -> {
                                    try (GzipCompressorOutputStream gcos = new GzipCompressorOutputStream(outputStream)) {
                                        try (TarArchiveOutputStream taos = new TarArchiveOutputStream(gcos)) {
                                            aggregateTarGzEntries(file.toPath(), file, taos);
                                        }
                                    }
                                });
                    }
                })
                .orElse(null);
    }

    private void aggregateTarGzEntries(Path root, File current, TarArchiveOutputStream taos) throws IOException {
        var files = current.listFiles();
        if (files != null) {
            for (var file : files) {
                if (file.isFile()) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        System.out.println(root.relativize(file.toPath()));
                        taos.putArchiveEntry(taos.createArchiveEntry(file, root.relativize(file.toPath()).toString()));
                        fis.transferTo(taos);
                    } finally {
                        taos.closeArchiveEntry();
                    }
                } else if (file.isDirectory()) {
                    aggregateTarGzEntries(root, file, taos);
                }
            }
        }
    }

    @RequestMapping(value = {"/files/resources/**"}, method = RequestMethod.OPTIONS)
    public Iterable<FileInfo> listResourceDirectory(
            HttpServletRequest request,
            User user,
            @RequestParam(value = "aggregateSizeForDirectories", required = false, defaultValue = "false") boolean aggregateSizeForDirectories) {
        return listDirectory(
                request,
                user,
                resourceManager.getResourceDirectory().orElseThrow(),
                Path.of("/resources/"),
                aggregateSizeForDirectories
        );
    }

    @RequestMapping(value = {"/files/workspaces/**"}, method = RequestMethod.OPTIONS)
    public Iterable<FileInfo> listWorkspaceDirectory(
            HttpServletRequest request,
            User user,
            @RequestParam(value = "aggregateSizeForDirectories", required = false, defaultValue = "false") boolean aggregateSizeForDirectories) {
        return listDirectory(
                request,
                user,
                resourceManager.getWorkspacesDirectory().orElseThrow(),
                Path.of("/workspaces/"),
                aggregateSizeForDirectories
        );
    }

    private Iterable<FileInfo> listDirectory(
            HttpServletRequest request,
            User user,
            @Nonnull Path directory,
            @Nonnull Path resolveTo,
            boolean aggregateSizeForDirectories) {
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
                                             .orElseGet(() -> {
                                                 if (aggregateSizeForDirectories && user.isSuperUser()) {
                                                     return aggregateSize(file);
                                                 } else {
                                                     return null;
                                                 }
                                             })
                             )
                        )
                        .collect(Collectors.toUnmodifiableList())
                )
                .orElse(Collections.emptyList());
    }

    private long aggregateSize(@Nonnull File directory) {
        var entries = directory.listFiles();
        var size    = 0L;
        if (entries != null) {
            for (var entry : entries) {
                if (entry.isFile()) {
                    size += entry.length();
                } else if (entry.isDirectory()) {
                    size += aggregateSize(entry);
                }
            }
        }
        return size;
    }

    @PatchMapping(value = {"/files/resources/**"})
    public ResponseEntity<String> patchResources(
            @Nonnull HttpServletRequest request,
            @Nonnull User user,
            @Nonnull @RequestBody Map<String, Object> options) {
        return patch(request, user, options, resourceManager.getResourceDirectory().orElseThrow());
    }

    @PatchMapping(value = {"/files/workspaces/**"})
    public ResponseEntity<String> patchWorkspaces(
            @Nonnull HttpServletRequest request,
            @Nonnull User user,
            @Nonnull @RequestBody Map<String, Object> options) {
        return patch(request, user, options, resourceManager.getWorkspacesDirectory().orElseThrow());
    }

    public ResponseEntity<String> patch(
            @Nonnull HttpServletRequest request,
            @Nonnull User user,
            @Nonnull Map<String, Object> options,
            @Nonnull Path directory) {
        try {
            var path = normalizedPath(request)
                    .map(directory::resolve)
                    .filter(p -> p.getNameCount() > 1) // prevent modification of the top level directories
                    .filter(p -> checker.isAllowedToAccessPath(user, p))
                    .orElseThrow();

            var optionRenameTo = options.get("rename-to");
            if (optionRenameTo instanceof String) {
                var renameTo = (String) optionRenameTo;
                var target   = path.resolveSibling(renameTo);
                if (target.getParent().equals(path.getParent())) {
                    Files.move(path, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    return ResponseEntity.badRequest().body("");
                }
            }

            return ResponseEntity.ok("");
        } catch (Throwable t) {
            t.printStackTrace();
            // blame everything on the user!
            return ResponseEntity.badRequest().body("");
        }
    }

    @Nonnull
    public static Optional<Path> normalizedPath(HttpServletRequest request) {
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

}
