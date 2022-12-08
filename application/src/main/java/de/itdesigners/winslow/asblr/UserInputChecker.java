package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.config.StageWorkerDefinition;
import de.itdesigners.winslow.config.UserInput;
import de.itdesigners.winslow.pipeline.Pipeline;
import de.itdesigners.winslow.pipeline.Submission;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserInputChecker implements AssemblerStep {

    private           boolean                     resetResumeNotification = false;
    private @Nullable Pipeline.ResumeNotification resumeNotification      = null;

    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        var pipeline           = context.getPipeline();
        var pipelineDefinition = context.getPipelineDefinition();
        var stageDefinition    = context.getStageDefinition();

        var requiresConfirmation = isConfirmationRequiredForNextStage(pipelineDefinition, stageDefinition, pipeline);
        var missingUserInput = hasMissingUserInput(
                pipelineDefinition,
                stageDefinition,
                context.getSubmission()
        ).collect(Collectors.toList());

        if (!missingUserInput.isEmpty()) {
            throw new MissingUserInputException(missingUserInput);
        }

        if (requiresConfirmation) {
            if (!isConfirmed(pipeline)) {
                throw new MissingUserConfirmationException();
            } else {
                resetResumeNotification = true;
                resumeNotification      = pipeline.getResumeNotification().orElse(null);
                pipeline.resetResumeNotification();
            }
        }
    }

    @Override
    public void revert(@Nonnull Context context) {
        if (resetResumeNotification) {
            context.getPipeline().resume(resumeNotification);
        }
    }

    private static boolean isConfirmed(@Nonnull Pipeline pipeline) {
        return pipeline.getResumeNotification().map(notification -> {
            switch (notification) {
                case Confirmation:
                case RunSingleThenPause:
                    return Boolean.TRUE;
                default:
                    return Boolean.FALSE;
            }
        }).orElse(Boolean.FALSE);
    }

    private static Stream<String> hasMissingUserInput(
            @Nonnull PipelineDefinition pipelineDefinition,
            @Nonnull StageDefinition stageDefinition,
            @Nonnull Submission submission) {
        if (stageDefinition instanceof StageWorkerDefinition stageWorkerDefinition) {
            return Stream.concat(
                    pipelineDefinition.userInput().getRequiredEnvVariables().stream(),
                    stageWorkerDefinition.userInput().getRequiredEnvVariables().stream()
            ).filter(k -> submission.getEnvVariable(k).isEmpty());
        } else {
            return Stream.empty();
        }
    }

    private static boolean isConfirmationRequiredForNextStage(
            @Nonnull PipelineDefinition pipelineDefinition,
            @Nonnull StageDefinition stageDefinition,
            @Nonnull Pipeline pipeline) {
        if (stageDefinition instanceof StageWorkerDefinition stageWorkerDefinition) {
            return Stream
                    .of(stageWorkerDefinition.userInput(), pipelineDefinition.userInput())
                    .filter(u -> u.getConfirmation() != UserInput.Confirmation.Never)
                    .anyMatch(u -> u.getConfirmation() == UserInput.Confirmation.Always || (
                                      u.getConfirmation() == UserInput.Confirmation.Once && pipeline
                                              .getActiveAndPastExecutionGroups()
                                              .noneMatch(g -> g.getStageDefinition().id().equals(stageDefinition.id()))
                              )
                    );
        } else {
            return false;
        }
    }

    public static class FurtherUserInputRequiredException extends AssemblyException {

        public FurtherUserInputRequiredException(String message) {
            super(message);
        }
    }

    public static class MissingUserInputException extends FurtherUserInputRequiredException {
        @Nonnull private final List<String> missing;

        public MissingUserInputException(@Nonnull List<String> missing) {
            super("This stage requires further input: " + String.join(", ", missing));
            this.missing = missing;
        }

        @Nonnull
        public List<String> getMissing() {
            return missing;
        }
    }

    public static class MissingUserConfirmationException extends FurtherUserInputRequiredException {
        public MissingUserConfirmationException() {
            super("A user confirmation is missing");
        }
    }
}
