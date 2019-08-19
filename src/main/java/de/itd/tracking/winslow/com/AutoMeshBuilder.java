package de.itd.tracking.winslow.com;

import java.net.SocketException;

public class AutoMeshBuilder {

    public static final int DEFAULT_PORT = 10007; // inspired by port 7 which is the echo-service
    public static final int DEFAULT_REACH_TIMEOUT_MS = 1000; // 1s
    public static final String DEFAULT_NAME = "AutoMesh";

    private int port;
    private String name;
    private int reachTimeoutMs;

    public AutoMeshBuilder() {
        this.onPort(DEFAULT_PORT)
            .withName(DEFAULT_NAME)
            .withReachTimeout(DEFAULT_REACH_TIMEOUT_MS);
    }

    public AutoMeshBuilder onPort(int port) {
        this.port = port;
        return this;
    }

    public AutoMeshBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public AutoMeshBuilder withReachTimeout(int reachTimeoutMs) {
        this.reachTimeoutMs = reachTimeoutMs;
        return this;
    }

    public AutoMesh build() throws SocketException {
        AutoMesh auto = new AutoMesh(this.port, reachTimeoutMs);
        Thread thread = new Thread(auto, this.name);
        thread.setDaemon(true);
        thread.start();
        return auto;
    }
}
