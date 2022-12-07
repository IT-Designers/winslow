package de.itdesigners.winslow.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.jaxrs.JerseyDockerHttpClient;
import de.itdesigners.winslow.Backend;
import de.itdesigners.winslow.BackendBuilder;
import de.itdesigners.winslow.node.PlatformInfo;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class DockerBackendBuilder implements BackendBuilder {

    private final @Nonnull String       nodeName;
    private final @Nonnull DockerClient dockerClient;

    public DockerBackendBuilder(@Nonnull String nodeName) {
        this.nodeName = nodeName;

        var dockerClientConfig = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .build();

        this.dockerClient = DockerClientImpl.getInstance(
                dockerClientConfig,
                new JerseyDockerHttpClient.Builder()
                        .dockerHost(dockerClientConfig.getDockerHost())
                        .sslConfig(dockerClientConfig.getSSLConfig())
                        .build()
        );

        this.dockerClient.pingCmd().exec();
    }

    @Nonnull
    @Override
    public Optional<PlatformInfo> tryRetrievePlatformInfoNoThrows() {
        return Optional.of(retrievePlatformInfoNoThrows());
    }

    @Nonnull
    protected PlatformInfo retrievePlatformInfoNoThrows() {
        Integer maxFreq = null;
        try {
            maxFreq = Integer.parseInt(Files.readString(Path.of(
                    "/sys/devices/system/cpu/cpufreq/policy0/cpuinfo_max_freq"
            )).trim());
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
        return new PlatformInfo(maxFreq);
    }

    @Nonnull
    @Override
    public Backend create() throws IOException {
        return new DockerBackend(nodeName, dockerClient, retrievePlatformInfoNoThrows());
    }
}
