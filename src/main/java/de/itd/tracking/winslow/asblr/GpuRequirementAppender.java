package de.itd.tracking.winslow.asblr;

import javax.annotation.Nonnull;

public class GpuRequirementAppender implements AssemblerStep
{
    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        context.getEnqueuedStage().getDefinition().getRequirements().ifPresent(requirements -> {
            requirements.getGpu().ifPresent(gpu -> {
                var builder = context.getBuilder().withGpuCount(gpu.getCount());

                if (gpu.getVendor().isPresent()) {
                    builder = builder.withGpuVendor(gpu.getVendor().get());
                }
            });
        });
    }

    @Override
    public void revert(@Nonnull Context context) {
        // nothing to revert
    }
}
