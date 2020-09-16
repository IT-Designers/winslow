package de.itdesigners.winslow;


import de.itdesigners.winslow.fs.Event;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.WorkDirectoryConfiguration;
import de.itdesigners.winslow.pipeline.Pipeline;
import org.javatuples.Pair;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class PipelineRepository extends BaseRepository {

    public static final String FILE_SUFFIX = ".pipeline" + FILE_EXTENSION;

    private final @Nonnull List<Consumer<Pair<String, Handle<Pipeline>>>> changeListeners
            = Collections.synchronizedList(new ArrayList<>());


    public PipelineRepository(
            @Nonnull LockBus lockBus,
            @Nonnull WorkDirectoryConfiguration workDirectoryConfiguration) throws IOException {
        super(lockBus, workDirectoryConfiguration);
        registerLockBusChangeListener();
    }

    private void registerLockBusChangeListener() {
        this.lockBus.registerEventListener(Event.Command.RELEASE, event -> {
            getProjectIdFromLockEventSubject(Path.of(event.getSubject())).ifPresent(projectId -> {
                var handle = getPipeline(projectId);
                if (handle.exists()) {
                    var pair = new Pair<>(projectId, handle);
                    this.changeListeners.forEach(listener -> listener.accept(pair));
                }

            });
        });
    }

    public void registerPipelineChangeListener(@Nonnull Consumer<Pair<String, Handle<Pipeline>>> changeListener) {
        this.changeListeners.add(changeListener);
    }

    public void removePipelineChangeListener(@Nonnull Consumer<Pair<String, Handle<Pipeline>>> changeListener) {
        this.changeListeners.remove(changeListener);
    }

    @Nonnull
    @Override
    protected Path getRepositoryDirectory() {
        return workDirectoryConfiguration.getPath().resolve("projects");
    }

    @Nonnull
    public Stream<Handle<Pipeline>> getAllPipelines() {
        return listAll(FILE_SUFFIX)
                .filter(path -> path.toString().endsWith(FILE_SUFFIX))
                .map(path -> createHandle(path, Pipeline.class));
    }

    @Nonnull
    public Handle<Pipeline> getPipeline(@Nonnull String projectId) {
        return getPipeline(getRepositoryFile(projectId, FILE_SUFFIX));
    }

    @Nonnull
    private Handle<Pipeline> getPipeline(@Nonnull Path path) {
        return createHandle(path, Pipeline.class);
    }

    @Nonnull
    private Optional<String> getProjectIdFromLockEventSubject(@Nonnull Path path) {
        var pathDirectory = path.getParent();
        var fileName = path.getFileName().toString();
        if (getRepositoryDirectory().endsWith(pathDirectory) && fileName.endsWith(FILE_SUFFIX)) {
            return Optional.of(fileName.substring(0, fileName.length() - FILE_SUFFIX.length()));
        } else {
            return Optional.empty();
        }
    }
}
