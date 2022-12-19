package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.config.StageWorkerDefinition;
import de.itdesigners.winslow.pipeline.DockerImage;

import javax.annotation.Nonnull;

public class DockerImageAppender implements AssemblerStep {
    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {

        if (context.getStageDefinition() instanceof StageWorkerDefinition stageWorker) {
            var image = stageWorker.image();
            var submission = context.getSubmission().withExtension(new DockerImage(
                    image.getName(),
                    image.getArgs(),
                    image.getShmSizeMegabytes().orElse(null),
                    stageWorker.privileged()
            ));
        } else {
            throw new RuntimeException(
                    "Can only assemble StageWorkerDefinitions for Docker Environment, not "
                            + context.getStageDefinition().getClass().getSimpleName()
            );
        }
    }

    @Override
    public void revert(@Nonnull Context context) {
        context.delete(DockerImage.class);
    }
}
