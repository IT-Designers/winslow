package de.itdesigners.winslow.web.websocket;

import javax.annotation.Nonnull;

public interface Pollable {

    void poll();

    void close();

    default void pollAndClose() {
        this.poll();
        this.close();
    }
}
