package de.itdesigners.winslow.node;

import javax.annotation.Nonnull;
import java.io.IOException;

public interface Node {

    @Nonnull
    String getName();

    @Nonnull
    NodeInfo loadInfo() throws IOException;
}
