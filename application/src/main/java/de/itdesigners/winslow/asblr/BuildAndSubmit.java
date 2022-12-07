package de.itdesigners.winslow.asblr;

import de.itdesigners.winslow.Backend;
import de.itdesigners.winslow.pipeline.SubmissionResult;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.logging.Level;

public class BuildAndSubmit implements AssemblerStep {

    private final @Nonnull Backend                    backend;
    private final @Nonnull String                     nodeName;
    private final @Nonnull Consumer<SubmissionResult> stageConsumer;

    public BuildAndSubmit(
            @Nonnull Backend backend,
            @Nonnull String nodeName,
            @Nonnull Consumer<SubmissionResult> stageConsumer) {
        this.backend       = backend;
        this.nodeName      = nodeName;
        this.stageConsumer = stageConsumer;
    }

    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        try {
            var result = context.getSubmission().submit(backend);
            context.log(Level.INFO, "Stage scheduled on node " + this.nodeName);
            this.stageConsumer.accept(result);
        } catch (IOException e) {
            throw new AssemblyException("Failed to submit", e);
        }
    }

    @Override
    public void revert(@Nonnull Context context) {

    }
}
