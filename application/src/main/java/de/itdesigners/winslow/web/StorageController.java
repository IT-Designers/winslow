package de.itdesigners.winslow.web;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.storage.StorageInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class StorageController {

    private static final Logger LOG = Logger.getLogger(StorageController.class.getSimpleName());

    private final Winslow winslow;

    public StorageController(Winslow winslow) {
        this.winslow = winslow;
    }

    @GetMapping("/storage")
    public Stream<StorageInfo> get() {
        var dir = winslow.getWorkDirectoryConfiguration().getPath();
        try (var files = Files.list(dir)) {
            var fileSys = new HashMap<FileStore, Path>(8);
            var stream = Stream.concat(
                    Stream.of(dir),
                    files
                            .collect(Collectors.toUnmodifiableList())
                            .stream()
                            .filter(f -> f.toFile().isDirectory())
            );

            return stream
                    .flatMap(f -> {
                        try {
                            FileStore fs  = Files.getFileStore(f);
                            var       old = fileSys.get(fs);
                            if (old == null || old.getNameCount() > f.getNameCount()) {
                                fileSys.put(fs, f);
                            }
                            return Stream.of(fs);
                        } catch (IOException e) {
                            LOG.log(Level.WARNING, "Failed to retrieve FileStore for " + f, e);
                            return Stream.empty();
                        }
                    })
                    .distinct()
                    .flatMap(fileStore -> {
                        try {
                            var total  = fileStore.getTotalSpace();
                            var usable = fileStore.getUsableSpace();
                            return Stream.of(
                                    new StorageInfo(
                                            Path.of("/", dir.relativize(fileSys.get(fileStore)).toString()).toString(),
                                            total - usable,
                                            usable
                                    ));
                        } catch (IOException e) {
                            LOG.log(
                                    Level.WARNING,
                                    "Failed to retrieve file system information for " + fileStore,
                                    e
                            );
                            return Stream.empty();
                        }
                    });
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to list files in workd directory", e);
            return Stream.empty();
        }
    }

}
