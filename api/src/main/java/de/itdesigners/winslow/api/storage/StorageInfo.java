package de.itdesigners.winslow.api.storage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StorageInfo {

    public final @Nullable String name;
    public final           long   bytesUsed;
    public final           long   bytesFree;

    public StorageInfo(@Nonnull String name, long bytesUsed, long bytesFree) {
        this.name      = name;
        this.bytesUsed = bytesUsed;
        this.bytesFree = bytesFree;
    }
}
