package de.itdesigners.winslow.api.node;

import de.itdesigners.winslow.api.Build;

import javax.annotation.Nonnull;

public record BuildInfo(
        @Nonnull String date,
        @Nonnull String commitHashShort,
        @Nonnull String commitHashLong
) {

    public BuildInfo() {
        this(
                Build.DATE,
                Build.COMMIT_HASH_SHORT,
                Build.COMMIT_HASH_LONG
        );
    }
}
