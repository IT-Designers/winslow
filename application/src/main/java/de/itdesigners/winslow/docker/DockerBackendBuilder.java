package de.itdesigners.winslow.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.jaxrs.JerseyDockerHttpClient;
import de.itdesigners.winslow.Backend;
import de.itdesigners.winslow.BackendBuilder;
import de.itdesigners.winslow.node.PlatformInfo;

import javax.annotation.Nonnull;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DockerBackendBuilder implements BackendBuilder {

    private final @Nonnull String             nodeName;
    private final @Nonnull DockerClientConfig dockerClientConfig;
    private final @Nonnull DockerClient       dockerClient;

    public DockerBackendBuilder(@Nonnull String nodeName) {
        this.nodeName           = nodeName;
        this.dockerClientConfig = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .build();

        this.dockerClient = DockerClientImpl.getInstance(
                this.dockerClientConfig,
                new JerseyDockerHttpClient.Builder()
                        .dockerHost(this.dockerClientConfig.getDockerHost())
                        .sslConfig(this.dockerClientConfig.getSSLConfig())
                        .build()
        );

        this.dockerClient.pingCmd().exec();
    }

    @Nonnull
    @Override
    public Optional<PlatformInfo> tryRetrievePlatformInfoNoThrows() {
        // TODO
        return Optional.of(new PlatformInfo(null));
    }

    @Nonnull
    @Override
    public Backend create() throws IOException {
        return new DockerBackend(dockerClient);
    }
}
