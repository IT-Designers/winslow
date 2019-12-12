package de.itdesigners.winslow.asblr;

import javax.annotation.Nonnull;

public class RequirementAppender implements AssemblerStep {
    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        context.getEnqueuedStage().getDefinition().getRequirements().ifPresent(requirements -> {
            requirements.getGpu().ifPresent(gpu -> {
                var builder = context
                        .getBuilder()
                        .withGpuCount(gpu.getCount());

                if (gpu.getVendor().isPresent()) {
                    builder = builder.withGpuVendor(gpu.getVendor().get());
                }
            });

            if (requirements.getMegabytesOfRam() > 0) {
                context.getBuilder().withMegabytesOfRam((int) requirements.getMegabytesOfRam());
            }
            if (requirements.getCpu() > 0) {
                context.getBuilder().withCpuCount(requirements.getCpu());
            }
        });
    }

    @Override
    public void revert(@Nonnull Context context) {
        // nothing to revert
    }
}
