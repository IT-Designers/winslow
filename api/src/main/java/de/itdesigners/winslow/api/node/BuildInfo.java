package de.itdesigners.winslow.api.node;

import de.itdesigners.winslow.api.Build;

import javax.annotation.Nonnull;

public class BuildInfo {

    private final @Nonnull String date;
    private final @Nonnull String commitHashShort;
    private final @Nonnull String commitHashLong;

    public BuildInfo() {
        this.date            = Build.DATE;
        this.commitHashShort = Build.COMMIT_HASH_SHORT;
        this.commitHashLong  = Build.COMMIT_HASH_LONG;
    }

    @Nonnull
    public String getDate() {
        return date;
    }

    @Nonnull
    public String getCommitHashShort() {
        return commitHashShort;
    }

    @Nonnull
    public String getCommitHashLong() {
        return commitHashLong;
    }
}
