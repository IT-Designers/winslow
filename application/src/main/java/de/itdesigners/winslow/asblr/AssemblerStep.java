package de.itdesigners.winslow.asblr;

import javax.annotation.Nonnull;

public interface AssemblerStep {

    default boolean applicable(@Nonnull Context context) {
        return true;
    }

    /**
     * Instructs this step to assemble itself or throw an
     * {@link AssemblyException} if pre-conditions are not met
     * or the operation failed
     * @param context The {@link Context} to operate on
     * @throws AssemblyException {@link AssemblyException} if the operation failed
     */
    void assemble(@Nonnull Context context) throws AssemblyException;

    /**
     * Reverts this step, not allowed to fail
     * @param context The {@link Context} to operate on
     */
    void revert(@Nonnull Context context);
}
