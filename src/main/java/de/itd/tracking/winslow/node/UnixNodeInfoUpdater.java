package de.itd.tracking.winslow.node;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class UnixNodeInfoUpdater implements Runnable {

    @Nonnull private final String name;
    @Nonnull private final Path   directory;

    public static final Path PROC = Path.of("/", "proc");

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

    private Path resolve(String name) {
        return directory.resolve(this.name + "." + name);
    }

    private void oneTimeCopies() {
        try {
            Files.copy(PROC.resolve("cpuinfo"), directory.resolve(name + ".cpuinfo"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        oneTimeCopies();
        while (true) {
            try {
                Thread.sleep(1_000);
                var stat = PROC.resolve("stat");
                var memi = PROC.resolve("meminfo");
                if (Files.exists(resolve("stat.1"))) {
                    Files.copy(resolve("stat.1"), resolve("stat.0"), StandardCopyOption.REPLACE_EXISTING);
                }
                Files.copy(stat, resolve("stat.1"), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(memi, resolve("meminfo"), StandardCopyOption.REPLACE_EXISTING);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}
