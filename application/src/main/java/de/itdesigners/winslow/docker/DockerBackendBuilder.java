package de.itdesigners.winslow.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import de.itdesigners.winslow.Backend;
import de.itdesigners.winslow.BackendBuilder;
import de.itdesigners.winslow.node.PlatformInfo;

import javax.annotation.Nonnull;
import java.io.IOException;

public class DockerBackendBuilder implements BackendBuilder {

    private final @Nonnull String       nodeName;
    private final @Nonnull DockerClient dockerClient;
    private final @Nonnull PlatformInfo platformInfo;

    public DockerBackendBuilder(@Nonnull String nodeName, @Nonnull PlatformInfo platformInfo) {
        this.nodeName     = nodeName;
        this.platformInfo = platformInfo;

        var dockerClientConfig = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .build();

        this.dockerClient = DockerClientImpl.getInstance(
                dockerClientConfig,
                new ApacheDockerHttpClient.Builder()
                        .dockerHost(dockerClientConfig.getDockerHost())
                        .sslConfig(dockerClientConfig.getSSLConfig())
                        .build()
        );

        this.dockerClient.pingCmd().exec();
    }

    @Nonnull
    @Override
    public Backend create() throws IOException {
        return new DockerBackend(nodeName, dockerClient, platformInfo);
    }
}
