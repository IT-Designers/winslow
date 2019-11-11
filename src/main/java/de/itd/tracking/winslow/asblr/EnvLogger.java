package de.itd.tracking.winslow.asblr;

import javax.annotation.Nonnull;

public class EnvLogger implements AssemblerStep {
    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        context.getBuilder().getEnvVariableKeys().forEach(context::logEnv);
    }

    @Override
    public void revert(@Nonnull Context context) {
        // nothing to undo
    }
}
