package de.itd.tracking.winslow.asblr;

import de.itd.tracking.winslow.OrchestratorException;
import de.itd.tracking.winslow.pipeline.Stage;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.logging.Level;

public class BuildAndSubmit implements AssemblerStep {

    @Nonnull private final String          nodeName;
    @Nonnull private final Consumer<Stage> stageConsumer;

    public BuildAndSubmit(
            @Nonnull String nodeName,
            @Nonnull Consumer<Stage> stageConsumer) {
        this.nodeName      = nodeName;
        this.stageConsumer = stageConsumer;
    }

    @Override
    public void assemble(@Nonnull Context context) throws AssemblyException {
        var stage         = (Stage) null;
        var prepared      = context.getBuilder().build();
        var stageEnqueued = context.getEnqueuedStage();

        try {
            switch (stageEnqueued.getAction()) {
                case Execute:
                    stage = prepared.execute();
                    context.log(Level.INFO, "Stage execution scheduled on node " + this.nodeName);
                    break;

                case Configure:
                    stage = prepared.configure();
                    context.log(Level.INFO, "Stage configured on node " + this.nodeName);
                    context.finishedEarly();
                    break;

                default:
                    throw new AssemblyException("Unexpected Stage Action " + stageEnqueued.getAction());
            }
        } catch (OrchestratorException e) {
            throw new AssemblyException("Failed to execute action", e);
        }

        this.stageConsumer.accept(stage);
    }

    @Override
    public void revert(@Nonnull Context context) {

    }
}
