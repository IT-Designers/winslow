package de.itdesigners.winslow.api.storage;

import javax.annotation.Nonnull;

public record StorageInfo(
        @Nonnull String name,
        long bytesUsed,
        long bytesFree) {

}
