package de.itdesigners.winslow;


import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.fs.Event;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.WorkDirectoryConfiguration;
import de.itdesigners.winslow.pipeline.Pipeline;
import org.javatuples.Pair;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PipelineRepository extends BaseRepository {

    public static final String FILE_SUFFIX         = ".pipeline" + FILE_EXTENSION;
    public static final String SIBLING_EG_HISTORY  = ".history" + FILE_EXTENSION;
    public static final String SIBLING_EG_ENQUEUED = ".enqueued" + FILE_EXTENSION;
    public static final String SIBLING_EG_ACTIVE   = ".active" + FILE_EXTENSION;

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
                .map(this::getPipeline);
    }

    @Nonnull
    public Handle<Pipeline> getPipeline(@Nonnull String projectId) {
        return getPipeline(getRepositoryFile(projectId, FILE_SUFFIX));
    }

    @Nonnull
    private Handle<Pipeline> getPipeline(@Nonnull Path path) {

        return createHandle(path, inputStream -> {
            var pipelineWithoutExecutionGroups = defaultReader(Pipeline.class).load(inputStream);
            return new Pipeline(
                    pipelineWithoutExecutionGroups.getProjectId(),
                    Stream.concat(
                            pipelineWithoutExecutionGroups.getExecutionHistory(),
                            Arrays.stream(loadExecutionGroups(getPipelineSiblingFile(path, SIBLING_EG_HISTORY)))
                    ).collect(Collectors.toList()),
                    Stream.concat(
                            pipelineWithoutExecutionGroups.getEnqueuedExecutions(),
                            Arrays.stream(loadExecutionGroups(getPipelineSiblingFile(path, SIBLING_EG_ENQUEUED)))
                    ).collect(Collectors.toList()),
                    Stream.concat(
                            pipelineWithoutExecutionGroups.getActiveExecutionGroups(),
                            Arrays.stream(loadExecutionGroups(getPipelineSiblingFile(path, SIBLING_EG_ACTIVE)))
                    ).collect(Collectors.toList()),
                    pipelineWithoutExecutionGroups.isPauseRequested(),
                    pipelineWithoutExecutionGroups.getPauseReason().orElse(null),
                    pipelineWithoutExecutionGroups.getResumeNotification().orElse(null),
                    pipelineWithoutExecutionGroups.getDeletionPolicy().orElse(null),
                    pipelineWithoutExecutionGroups.getWorkspaceConfigurationMode().orElse(null),
                    pipelineWithoutExecutionGroups.getExecutionCounter()
            );
        }, (outputStream, pipelineWithExecutionGroups) -> {
            storeExecutionGroups(
                    getPipelineSiblingFile(path, SIBLING_EG_HISTORY),
                    pipelineWithExecutionGroups.getExecutionHistory()
            );
            storeExecutionGroups(
                    getPipelineSiblingFile(path, SIBLING_EG_ENQUEUED),
                    pipelineWithExecutionGroups.getEnqueuedExecutions()
            );
            storeExecutionGroups(
                    getPipelineSiblingFile(path, SIBLING_EG_ACTIVE),
                    pipelineWithExecutionGroups.getActiveExecutionGroups()
            );
            defaultWriter().store(outputStream, new Pipeline(
                    pipelineWithExecutionGroups.getProjectId(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    pipelineWithExecutionGroups.isPauseRequested(),
                    pipelineWithExecutionGroups.getPauseReason().orElse(null),
                    pipelineWithExecutionGroups.getResumeNotification().orElse(null),
                    pipelineWithExecutionGroups.getDeletionPolicy().orElse(null),
                    pipelineWithExecutionGroups.getWorkspaceConfigurationMode().orElse(null),
                    pipelineWithExecutionGroups.getExecutionCounter()
            ));
        });
    }

    @Nonnull
    private ExecutionGroup[] loadExecutionGroups(@Nonnull Path path) throws IOException {
        try (var fis = new FileInputStream(path.toFile())) {
            return Optional
                    .ofNullable(defaultReader(ExecutionGroup[].class).load(fis))
                    .orElse(new ExecutionGroup[0]);
        } catch (FileNotFoundException e) {
            return new ExecutionGroup[0];
        }
    }

    private void storeExecutionGroups(@Nonnull Path path, @Nonnull Stream<ExecutionGroup> groups) throws IOException {
        try (var fos = new FileOutputStream(path.toFile())) {
            defaultWriter().store(fos, groups.toArray(ExecutionGroup[]::new));
        }
    }

    @Nonnull
    protected static Path getPipelineSiblingFile(@Nonnull Path path, @Nonnull String sibling) {
        var fileName = path.getFileName().toString();
        return path.resolveSibling(
                fileName.substring(0, fileName.length() - FILE_EXTENSION.length()) + sibling
        );
    }

    @Nonnull
    private Optional<String> getProjectIdFromLockEventSubject(@Nonnull Path path) {
        var pathDirectory = path.getParent();
        var fileName      = path.getFileName().toString();
        if (getRepositoryDirectory().endsWith(pathDirectory) && fileName.endsWith(FILE_SUFFIX)) {
            return Optional.of(fileName.substring(0, fileName.length() - FILE_SUFFIX.length()));
        } else {
            return Optional.empty();
        }
    }
}
