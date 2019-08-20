package de.itd.tracking.winslow;

import de.itd.tracking.winslow.fs.FileSystemConfiguration;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, String> variables = new HashMap<>();
    private final FileSystemConfiguration configuration;

    public Environment(FileSystemConfiguration configuration) {
        this.configuration = configuration;
    }

    public FileSystemConfiguration getConfiguration() {
        return configuration;
    }
}
