package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.Backend;
import de.itdesigners.winslow.NoOpStageHandle;
import de.itdesigners.winslow.pipeline.Stage;
import de.itdesigners.winslow.pipeline.StageAssignedWorkspace;
import de.itdesigners.winslow.pipeline.Submission;
import de.itdesigners.winslow.pipeline.SubmissionResult;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BuildAndSubmit implements AssemblerStep {

    private static final Logger LOG = Logger.getLogger(BuildAndSubmit.class.getSimpleName());

    private final @Nonnull Backend                    backend;
    private final @Nonnull String                     nodeName;
    private final @Nonnull Consumer<SubmissionResult> stageConsumer;

    public BuildAndSubmit(
            @Nonnull Backend backend,
            @Nonnull String nodeName,
            @Nonnull Consumer<SubmissionResult> stageConsumer) {
        this.backend       = backend;
        this.nodeName      = nodeName;
        this.stageConsumer = stageConsumer;
    }

    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        try {
            var result = new SubmissionResult(
                    createStage(
                            context.getSubmission(),
                            context
                                    .load(StageAssignedWorkspace.class)
                                    .map(StageAssignedWorkspace::absolutePath)
                                    .orElse(null)
                    ),
                    context.isConfigureOnly()
                    ? new NoOpStageHandle()
                    : backend.submit(context.getSubmission())
            );

            context.log(Level.INFO, "Stage scheduled on node " + this.nodeName);
            context.store(result);
            this.stageConsumer.accept(result);
        } catch (IOException e) {
            throw new AssemblyException("Failed to submit", e);
        }
    }


    @Nonnull
    private Stage createStage(@Nonnull Submission submission, @Nullable String workspaceDirectory) {
        var stage = new Stage(submission.getId(), workspaceDirectory);

        stage.getEnv().putAll(submission.getStageEnvVariablesReduced());
        stage.getEnvPipeline().putAll(submission.getPipelineEnvVariables());
        stage.getEnvSystem().putAll(submission.getSystemEnvVariables());
        stage.getEnvInternal().putAll(submission.getInternalEnvVariables());

        stage.getEnvPipeline().forEach((key, value) -> stage.getEnv().remove(key, value));
        stage.getEnvSystem().forEach((key, value) -> {
            if (!stage.getEnvPipeline().containsKey(key)) {
                stage.getEnv().remove(key, value);
            }
        });
        stage.getEnvInternal().forEach((key, value) -> stage.getEnv().remove(key, value));

        return stage;
    }

    @Override
    public void revert(@Nonnull Context context) {
        context.load(SubmissionResult.class).ifPresent(result -> {
            try (var handle = result.getHandle()) {
                handle.kill();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed to revert, could not kill or close handle", e);
            }
        });
    }
}
