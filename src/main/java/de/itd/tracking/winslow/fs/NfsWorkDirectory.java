package de.itd.tracking.winslow.fs;


import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Path;
import java.util.Optional;

public class NfsWorkDirectory implements WorkDirectoryConfiguration {

    private static final String NFS_TYPE_PATTERN = " nfs";

    private final Path   workDirectory;
    private final String serverAddress;
    private final String serverExport;
    private final String options;

    public NfsWorkDirectory(Path workDirectory, String serverAddress, String serverExport, String options) {
        this.workDirectory = workDirectory;
        this.serverAddress = serverAddress;
        this.serverExport = serverExport;
        this.options = options;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getServerExport() {
        return serverExport;
    }

    public String getOptions() {
        return options;
    }

    public static NfsWorkDirectory loadFromCurrentConfiguration(Path workDir) throws IOException {
        String pattern = " " + workDir.toFile().getCanonicalPath() + " ";
        String line;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(openOneOf("/etc/mtab",
                                                                                        "/proc/mounts"
                                                                                       )))) {
            while ((line = reader.readLine()) != null) {
                if (line.contains(pattern) && line.contains(NFS_TYPE_PATTERN)) {
                    try {
                        String[] segments        = line.split(" ");
                        String[] serverAndExport = segments[0].split(":");

                        return new NfsWorkDirectory(workDir, serverAndExport[0], serverAndExport[1], segments[3]);
                    } catch (IndexOutOfBoundsException e) {
                        throw new IOException("Failed to parse mount configuration: " + line);
                    }
                }
            }
        }
        throw new IOException("Working director '" + workDir + "' is not mounted");
    }

    private static FileInputStream openOneOf(String... files) throws IOException {
        for (int i = 0; i < files.length; ++i) {
            try {
                return new FileInputStream(files[i]);
            } catch (IOException e) {
                if (i + 1 >= files.length) {
                    throw e;
                }
                // ignore
            }
        }
        throw new IOException("No input file given");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{addr=" + serverAddress + ",export=" + serverExport + ",options=" + options + "}#" + hashCode();
    }

    @Nonnull
    @Override
    public Path getPath() {
        return workDirectory;
    }

    public Optional<Path> toExportedPath(Path workDirPath) {
        try {
            return Optional.of(new File(this.serverExport)
                                       .toPath()
                                       .resolve(this.workDirectory.relativize(workDirPath)));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }
}
