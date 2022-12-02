package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.pipeline.DockerImage;

import javax.annotation.Nonnull;

public class DockerImageAppender implements AssemblerStep {
    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        var image = context.getStageDefinition().image();

        var submission = context.getSubmission().withExtension(new DockerImage(
                image.getName(),
                image.getArgs(),
                image.getShmSizeMegabytes(),
                context.getStageDefinition().privileged()
        ));

    }

    @Override
    public void revert(@Nonnull Context context) {
        context.delete(DockerImage.class);
    }
}
