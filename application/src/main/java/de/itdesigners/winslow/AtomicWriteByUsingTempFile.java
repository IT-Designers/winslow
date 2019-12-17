package de.itdesigners.winslow;

import javax.annotation.Nonnull;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class AtomicWriteByUsingTempFile {

    public static final String TEMP_FILE_SUFFIX = ".tmp";

    public static void write(@Nonnull Path target, @Nonnull OutputStreamConsumer writer) throws IOException {
        var tmpFile = target.resolveSibling(target.getFileName().toString() + TEMP_FILE_SUFFIX);

        try {
            try (var fos = new FileOutputStream(tmpFile.toFile())) {
                writer.consume(fos);
            }

            Files.move(tmpFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

        } finally {
            Files.deleteIfExists(tmpFile);
        }
    }

    interface OutputStreamConsumer {
        void consume(@Nonnull OutputStream outputStream) throws IOException;
    }
}
