package de.itd.tracking.winslow.node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

public class UnixNodeInfoUpdater implements Runnable {

    @Nonnull private final String name;
    @Nonnull private final Path   directory;

    @Nullable private UnixProcStatParser.CpuTimes before = null;

    private UnixNodeInfoUpdater(@Nonnull String name, @Nonnull Path directory) {
        this.name      = name;
        this.directory = directory;
    }

    public static void spawn(@Nonnull String name, @Nonnull Path nodesDirectory) {
        var updater = new UnixNodeInfoUpdater(name, nodesDirectory);
        var thread  = new Thread(updater);
        thread.setName(UnixNodeInfoUpdater.class.getSimpleName() + "." + name);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1_000);
                var path = Path.of("/", "proc", "stat");
                Files.copy(directory.resolve(name + ".stat.1"), directory.resolve(name + ".stat.0"), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(path, directory.resolve(name + ".stat.1"), StandardCopyOption.REPLACE_EXISTING);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}
