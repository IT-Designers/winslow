package de.itd.tracking.winslow.resource;

import com.moandjiezana.toml.Toml;
import de.itd.tracking.winslow.config.Pipeline;
import de.itd.tracking.winslow.config.Stage;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceManager {

    private final Path              workDirectory;
    private final PathConfiguration configuration;

    public ResourceManager(Path workDirectory, PathConfiguration configuration) {
        this.workDirectory = workDirectory;
        this.configuration = configuration;
    }

    public Optional<Path> getWorkspacesDirectory() {
        Path path = configuration.resolvePathOfWorkspaces(workDirectory);
        if (!path.toFile().exists() && !path.toFile().mkdirs()) {
            return Optional.empty();
        } else {
            return Optional.of(path);
        }
    }

    /**
     * @param jobId The id of the job to retrieve the workspace for
     * @return The {@link Path} to the workspace for the given job id if it exists
     */
    public Optional<Path> getWorkspace(UUID jobId) {
        return getWorkspacesDirectory()
                .map(p -> p.resolve(jobId.toString()))
                .filter(p -> p.toFile().exists());
    }

    /**
     * @param jobId The id of the job to create the workspace for
     * @param failIfAlreadyExists Whether to return false if the directory already exists
     * @return Whether the create operation was successful
     */
    public Optional<Path> createWorkspace(UUID jobId, boolean failIfAlreadyExists) {
        return getWorkspacesDirectory()
                .map(p -> p.resolve(jobId.toString()))
                .filter(p -> p.toFile().mkdirs() == failIfAlreadyExists);
    }

    public Optional<Path> getResourceDirectory() {
        return Optional.of(configuration.resolvePathOfResources(workDirectory))
                .filter(p -> p.toFile().exists() || p.toFile().mkdirs());
    }

    public Optional<Path> getPipelinesDirectory() {
        return Optional.of(configuration.resolvePathOfPipelines(workDirectory))
                .filter(p -> p.toFile().exists() || p.toFile().mkdirs());
    }

    public Iterable<String> getPipelineIdentifiers() {
        String ENDING = ".toml";
        return getPipelinesDirectory()
                .stream()
                .flatMap(p -> {
                    var files = p.toFile().listFiles((file, name) -> name.endsWith(ENDING));
                    return files != null ? Arrays.stream(files) : Stream.empty();
                })
                .map(File::getName)
                .map(name -> name.substring(0, name.length() - ENDING.length()))
                .collect(Collectors.toUnmodifiableList());
    }

    public Optional<Pipeline> loadPipeline(String id) {
        String ENDING = ".toml";
        return getPipelinesDirectory()
                .map(p -> p.resolve(Path.of(id + ENDING).getFileName()))
                .map(p -> {
                    var toml = new Toml().read(p.toFile());
                    var stages = toml.getTables("stage").stream().map(table -> table.to(Stage.class)).collect(Collectors.toList());
                    var pipe = toml.getTable("pipeline").to(Pipeline.class);

                    return new Pipeline(
                            pipe.getName(),
                            pipe.getDescription().orElse(null),
                            pipe.getUserInput().orElse(null),
                            stages
                    );
                });
    }
}
