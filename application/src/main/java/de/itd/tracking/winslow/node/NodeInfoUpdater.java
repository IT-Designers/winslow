package de.itd.tracking.winslow.node;

import com.moandjiezana.toml.TomlWriter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

public class NodeInfoUpdater implements Runnable {

    public static final String TMP_FILE_SUFFIX = ".tmp";

    @Nonnull private final Path directory;
    @Nonnull private final Node node;

    private NodeInfoUpdater(@Nonnull Path directory, @Nonnull Node node) {
        this.directory = directory;
        this.node      = node;
    }

    public static void spawn(@Nonnull Path directory, @Nonnull Node node) {
        var updater = new NodeInfoUpdater(directory, node);
        var thread  = new Thread(updater);
        thread.setName(NodeInfoUpdater.class.getSimpleName() + "." + node.getName());
        thread.setDaemon(true);
        thread.start();
    }


    private Path getTmpPath() {
        var file = getFilePath();
        return file.resolveSibling(file.getFileName() + TMP_FILE_SUFFIX);
    }

    private Path getFilePath() {
        return directory.resolve(node.getName());
    }

    @Override
    public void run() {
        while (true) {
            try {
                var info = node.loadInfo();
                var file = getFilePath();
                var tmp  = getTmpPath();
                // update NodeRepository which also has hardcoded Toml
                new TomlWriter().write(info, tmp.toFile()); // TODO  do this through the repository
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, ATOMIC_MOVE);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
