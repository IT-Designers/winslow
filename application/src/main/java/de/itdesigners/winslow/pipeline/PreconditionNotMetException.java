package de.itdesigners.winslow.pipeline;

import javax.annotation.Nonnull;

public class PreconditionNotMetException extends Exception {

    public PreconditionNotMetException(String message) {
        super(message);
    }

    public static void requireFalse(@Nonnull String message, boolean value) throws PreconditionNotMetException {
        if (value) {
            throw new PreconditionNotMetException(message);
        }
    }

    public static void requireTrue(@Nonnull String message, boolean value) throws PreconditionNotMetException {
        if (!value) {
            throw new PreconditionNotMetException(message);
        }
    }
}
