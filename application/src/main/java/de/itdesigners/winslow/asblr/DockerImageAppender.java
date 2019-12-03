package de.itdesigners.winslow.asblr;

import javax.annotation.Nonnull;

public class DockerImageAppender implements AssemblerStep {
    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        context.getEnqueuedStage().getDefinition().getImage().ifPresent(image -> {
            context.getBuilder()
                   .withDockerImage(image.getName())
                   .withDockerImageArguments(image.getArgs());
        });
    }

    @Override
    public void revert(@Nonnull Context context) {
        // nothing to revert
    }
}
