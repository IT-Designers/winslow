package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.config.UserInput;
import de.itdesigners.winslow.pipeline.Pipeline;
import de.itdesigners.winslow.pipeline.Submission;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserInputChecker implements AssemblerStep {
    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        var pipeline           = context.getPipeline();
        var pipelineDefinition = context.getPipelineDefinition();
        var stageDefinition    = context.getEnqueuedStage().getDefinition();

        var requiresConfirmation = isConfirmationRequiredForNextStage(pipelineDefinition, stageDefinition, pipeline);
        var missingUserInput = hasMissingUserInput(
                pipelineDefinition,
                stageDefinition,
                context.getSubmission()
        ).collect(Collectors.toList());

        if (requiresConfirmation && isConfirmed(pipeline)) {
            requiresConfirmation = false;
        }

        if (!missingUserInput.isEmpty()) {
            throw new MissingUserInputException(missingUserInput);
        }

        if (requiresConfirmation) {
            throw new MissingUserConfirmationException();
        }
    }

    @Override
    public void revert(@Nonnull Context context) {
        // nothing to revert
    }

    private static boolean isConfirmed(@Nonnull Pipeline pipeline) {
        return Pipeline.ResumeNotification.Confirmation == pipeline.getResumeNotification().orElse(null);
    }

    private static Stream<String> hasMissingUserInput(
            @Nonnull PipelineDefinition pipelineDefinition,
            @Nonnull StageDefinition stageDefinition,
            @Nonnull Submission submission) {
        return Stream.concat(
                pipelineDefinition.getRequires().stream().flatMap(u -> u.getEnvironment().stream()),
                stageDefinition.getRequires().stream().flatMap(u -> u.getEnvironment().stream())
        ).filter(k -> submission.getEnvVariable(k).isEmpty());
    }

    private static boolean isConfirmationRequiredForNextStage(
            @Nonnull PipelineDefinition pipelineDefinition,
            @Nonnull StageDefinition stageDefinition,
            @Nonnull Pipeline pipeline) {
        return Stream
                .concat(stageDefinition.getRequires().stream(), pipelineDefinition.getRequires().stream())
                .filter(u -> u.getConfirmation() != UserInput.Confirmation.Never)
                .anyMatch(u -> !(u.getConfirmation() == UserInput.Confirmation.Once && pipeline
                        .getAllStages()
                        .anyMatch(s -> s.getDefinition().equals(stageDefinition))));
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
