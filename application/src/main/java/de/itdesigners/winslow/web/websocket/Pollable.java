package de.itdesigners.winslow.web.websocket;

public interface Pollable {

    void poll();

    void close();

    default void pollAndClose() {
        this.poll();
        this.close();
    }
}
