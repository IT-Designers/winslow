package de.itdesigners.winslow.node;

import de.itdesigners.winslow.api.node.NodeInfo;

import javax.annotation.Nonnull;
import java.io.IOException;

public interface Node {

    @Nonnull
    String getName();

    @Nonnull
    PlatformInfo getPlatformInfo();

    @Nonnull
    NodeInfo loadInfo() throws IOException;
}
