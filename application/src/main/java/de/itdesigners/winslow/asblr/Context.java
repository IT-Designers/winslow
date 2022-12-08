package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.Executor;
import de.itdesigners.winslow.api.pipeline.LogEntry;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.pipeline.Pipeline;
import de.itdesigners.winslow.pipeline.StageId;
import de.itdesigners.winslow.pipeline.Submission;
import de.itdesigners.winslow.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;

public class Context {

    private final @Nonnull Project project;
    private final @Nonnull Pipeline pipeline;
    private final @Nonnull  PipelineDefinition pipelineDefinition;
    private final @Nonnull  ExecutionGroup     executionGroup;
    private final @Nullable Executor           executor;
    private final @Nonnull  StageId            stageId;
    private final @Nonnull  Submission         submission;

    private final @Nonnull Map<Class<?>, Object> intermediateResults = new HashMap<>();

    public Context(
            @Nonnull Project project,
            @Nonnull Pipeline pipeline,
            @Nonnull PipelineDefinition definition,
            @Nonnull ExecutionGroup executionGroup,
            @Nullable Executor executor,
            @Nonnull StageId stageId,
            @Nonnull Submission submission) {
        this.project            = project;
        this.pipeline           = pipeline;
        this.pipelineDefinition = definition;
        this.executionGroup     = executionGroup;
        this.executor           = executor;
        this.stageId            = stageId;
        this.submission         = submission;
    }

    @Nonnull
    public Project getProject() {
        return project;
    }

    @Nonnull
    public StageId getStageId() {
        return stageId;
    }

    @Nonnull
    public Pipeline getPipeline() {
        return pipeline;
    }

    @Nonnull
    public PipelineDefinition getPipelineDefinition() {
        return pipelineDefinition;
    }

    @Nonnull
    public ExecutionGroup getExecutionGroup() {
        return executionGroup;
    }

    @Nonnull
    public Submission getSubmission() {
        return this.submission;
    }

    public <T> void store(@Nonnull T value) {
        this.intermediateResults.put(value.getClass(), value);
    }

    public <T> void store(@Nonnull Class<T> type, @Nonnull T value) {
        this.intermediateResults.put(type, value);
    }

    @Nonnull
    public <T> Optional<T> load(@Nonnull Class<T> type) {
        var value = this.intermediateResults.get(type);
        return Optional.ofNullable(type.cast(value));
    }

    @Nonnull
    public <T> T loadOrThrow(@Nonnull Class<T> type) throws AssemblyException {
        var value = this.intermediateResults.get(type);
        return Optional
                .ofNullable(type.cast(value))
                .orElseThrow(() -> new AssemblyException("Failed to load value for " + type));
    }

    /**
     * Logs an internal message with the given level
     *
     * @param level   Categorization of the log
     * @param message Message to log
     * @param t       {@link Throwable} to append to the logs
     */
    public void log(@Nonnull Level level, @Nonnull String message, @Nonnull Throwable t) {
        var baos = new ByteArrayOutputStream();
        try (var ps = new PrintStream(baos)) {
            t.printStackTrace(ps);
        }
        log(level, message);
        baos.toString(StandardCharsets.UTF_8).lines().forEach(line -> log(level, line));
    }

    /**
     * Logs an internal message with the given level
     *
     * @param level   Categorization of the log
     * @param message Message to log
     */
    public void log(@Nonnull Level level, @Nonnull String message) {
        if (this.executor != null) {
            if (level.intValue() == Level.INFO.intValue()) {
                this.executor.logInf(message);
            } else if (level.intValue() > Level.INFO.intValue()) {
                this.executor.logErr(message);
            }
        }
    }

    /**
     * @see Executor#addLogEntryConsumer(Consumer)
     */
    public void addLogListener(@Nonnull Consumer<LogEntry> listener) {
        if (this.executor != null) {
            this.executor.addLogEntryConsumer(listener);
        }
    }

    public void removeLogListener(@Nonnull Consumer<LogEntry> listener) {
        if (this.executor != null) {
            this.executor.removeLogEntryConsumer(listener);
        }
    }

    public boolean hasAssemblyBeenAborted() {
        return this.executor != null && this.executor.hasBeenKilled();
    }

    public void ensureAssemblyHasNotBeenAborted() throws AssemblyException {
        this.throwIfAssemblyHasBeenAborted();
    }

    public void throwIfAssemblyHasBeenAborted() throws AssemblyException {
        if (this.hasAssemblyBeenAborted()) {
            throw new AssemblyException("Stage assembly has been aborted");
        }
    }
}
