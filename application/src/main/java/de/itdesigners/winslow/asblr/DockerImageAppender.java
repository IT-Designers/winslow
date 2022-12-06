package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.config.StageWorkerDefinition;
import de.itdesigners.winslow.pipeline.DockerImage;

import javax.annotation.Nonnull;

public class DockerImageAppender implements AssemblerStep {
    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {

        if(context.getSubmission().getStageDefinition() instanceof StageWorkerDefinition stageWorker) {

            var image = stageWorker.image();

            var submission = context.getSubmission().withExtension(new DockerImage(
                    image.getName(),
                    image.getArgs(),
                    image.getShmSizeMegabytes(),
                    stageWorker.privileged()
            ));
        }
        else throw new RuntimeException("Can only assamble StageWorkerDefinitions for Docker Environment, not " + context.getSubmission().getStageDefinition().toString());
    }

    @Override
    public void revert(@Nonnull Context context) {
        // nothing to revert
    }
}
