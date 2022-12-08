package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.gateway.GatewayExtension;

import javax.annotation.Nonnull;

public class GatewayExtensionAppender implements AssemblerStep {

    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        if (context.getExecutionGroup().isGateway()) {
            context.getSubmission().withExtension(new GatewayExtension(context.getStageDefinition()));
        }
    }

    @Override
    public void revert(@Nonnull Context context) {

    }
}

