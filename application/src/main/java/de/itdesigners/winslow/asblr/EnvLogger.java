package de.itdesigners.winslow.asblr;

import javax.annotation.Nonnull;
import java.util.TreeSet;
import java.util.logging.Level;

public class EnvLogger implements AssemblerStep {
    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        var submission = context.getSubmission();
        var tree    = new TreeSet<String>();

        submission.getEnvVariableKeys().forEach(tree::add);


        if (tree.size() > 0) {
            context.log(Level.INFO, " The following environment variables are set");
            tree.forEach(env -> context.log(Level.INFO, "   - " + env + "=" + submission.getEnvVariable(env).orElse(null)));
        } else {
            context.log(Level.INFO, " No environment variables set");
        }

    }

    @Override
    public void revert(@Nonnull Context context) {
        // nothing to undo
    }
}
