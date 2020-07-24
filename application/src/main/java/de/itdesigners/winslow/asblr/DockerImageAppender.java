package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.pipeline.DockerImage;

import javax.annotation.Nonnull;

public class DockerImageAppender implements AssemblerStep {
    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        context.getSubmission().getStageDefinition().getImage().ifPresent(image -> {
            var submission = context.getSubmission().withExtension(new DockerImage(
                    image.getName(),
                    image.getArgs(),
                    image.getShmSizeMegabytes().orElse(null),
                    context.getSubmission().getStageDefinition().isPrivileged()
            ));
        });
    }

    @Override
    public void revert(@Nonnull Context context) {
        // nothing to revert
    }
}
