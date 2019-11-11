package de.itd.tracking.winslow.asblr;

import de.itd.tracking.winslow.Executor;
import de.itd.tracking.winslow.config.PipelineDefinition;
import de.itd.tracking.winslow.pipeline.EnqueuedStage;
import de.itd.tracking.winslow.pipeline.Pipeline;
import de.itd.tracking.winslow.pipeline.PreparedStageBuilder;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public class Context {

    @Nonnull private final Pipeline             pipeline;
    @Nonnull private final PipelineDefinition   pipelineDefinition;
    @Nonnull private final Executor             executor;
    @Nonnull private final EnqueuedStage        enqueuedStage;
    @Nonnull private final String               stageId;
    @Nonnull private final PreparedStageBuilder builder;

    @Nonnull private final Map<Class<?>, Object> intermediateResults = new HashMap<>();

    public Context(
            @Nonnull Pipeline pipeline,
            @Nonnull PipelineDefinition definition, @Nonnull Executor executor,
            @Nonnull EnqueuedStage enqueuedStage,
            @Nonnull String stageId, @Nonnull PreparedStageBuilder builder) {
        this.pipeline      = pipeline;
        pipelineDefinition = definition;
        this.executor      = executor;
        this.enqueuedStage = enqueuedStage;
        this.stageId       = stageId;
        this.builder       = builder;
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
    public EnqueuedStage getEnqueuedStage() {
        return enqueuedStage;
    }

    @Nonnull
    public PreparedStageBuilder getBuilder() {
        return builder;
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

    public void logEnv(@Nonnull String env) {
        this.log(Level.INFO, env + "=" + this.getBuilder().getEnvVariable(env).orElse(null));
    }

    public void log(@Nonnull Level level, @Nonnull String message) {
        if (level.intValue() == Level.INFO.intValue()) {
            this.executor.logInf(message);
        } else if (level.intValue() > Level.INFO.intValue()) {
            this.executor.logErr(message);
        }
    }
}
