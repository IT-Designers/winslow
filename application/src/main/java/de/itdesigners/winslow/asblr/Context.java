package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.Executor;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.pipeline.EnqueuedStage;
import de.itdesigners.winslow.pipeline.Pipeline;
import de.itdesigners.winslow.pipeline.Submission;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public class Context {

    private final @Nonnull  Pipeline           pipeline;
    private final @Nonnull  PipelineDefinition pipelineDefinition;
    private final @Nullable Executor           executor;
    private final @Nonnull  String             stageId;
    private final @Nonnull  Submission         submission;

    @Nonnull private final Map<Class<?>, Object> intermediateResults = new HashMap<>();

    public Context(
            @Nonnull Pipeline pipeline,
            @Nonnull PipelineDefinition definition,
            @Nullable Executor executor,
            @Nonnull String stageId,
            @Nonnull Submission submission) {
        this.pipeline      = pipeline;
        this.pipelineDefinition = definition;
        this.executor      = executor;
        this.stageId       = stageId;
        this.submission    = submission;
    }

    @Nonnull
    public String getStageId() {
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

    public void log(@Nonnull Level level, @Nonnull String message, @Nonnull Throwable t) {
        var baos = new ByteArrayOutputStream();
        try (var ps = new PrintStream(baos)) {
            t.printStackTrace(ps);
        }
        log(level, message);
        baos.toString(StandardCharsets.UTF_8).lines().forEach(line -> log(level, line));
    }

    public void log(@Nonnull Level level, @Nonnull String message) {
        if (this.executor != null) {
            if (level.intValue() == Level.INFO.intValue()) {
                this.executor.logInf(message);
            } else if (level.intValue() > Level.INFO.intValue()) {
                this.executor.logErr(message);
            }
        }
    }

    public void finishedEarly() {
        if (this.executor != null) {
            this.executor.stop();
        }
    }
}
