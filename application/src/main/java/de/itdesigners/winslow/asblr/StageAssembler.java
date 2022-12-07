package de.itdesigners.winslow.asblr;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StageAssembler {

    private static final Logger              LOG   = Logger.getLogger(StageAssembler.class.getSimpleName());
    private final        List<AssemblerStep> steps = new ArrayList<>();


    public StageAssembler() {

    }

    @Nonnull
    @CheckReturnValue
    public StageAssembler add(@Nonnull AssemblerStep step) {
        this.steps.add(step);
        return this;
    }

    public void assemble(@Nonnull Context context) throws AssemblyException {
        context.ensureAssemblyHasNotBeenAborted();
        context.log(Level.INFO, "Starting to assemble stage");

        for (int i = 0; i < this.steps.size(); ++i) {
            try {
                if (this.steps.get(i).applicable(context)) {
                    context.log(Level.INFO, "Assembly step " + this.steps.get(i).getClass().getSimpleName());
                    this.steps.get(i).assemble(context);
                    context.throwIfAssemblyHasBeenAborted();
                }
            } catch (AssemblyException e) {
                context.log(Level.SEVERE, "Assembly failed at index " + i, e);
                LOG.log(Level.SEVERE, "Assembly failed at index " + i, e);
                this.revert(context, i);
                throw e;
            }
        }
    }

    private void revert(@Nonnull Context context, int startAt) {
        context.log(Level.WARNING, "Going to revert assembly");
        for (int i = startAt; i >= 0; --i) {
            try {
                this.steps.get(i).revert(context);
            } catch (Throwable t) {
                context.log(Level.SEVERE, "Revert failed to step at index " + i + ", this is illegal!");
                LOG.log(Level.SEVERE, "Revert failed to step at index " + i + ", this is illegal!");
            }
        }
    }
}
