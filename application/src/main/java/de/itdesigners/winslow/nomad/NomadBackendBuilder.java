package de.itdesigners.winslow.nomad;

import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadApiConfiguration;
import de.itdesigners.winslow.BackendBuilder;
import de.itdesigners.winslow.node.PlatformInfo;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NomadBackendBuilder implements BackendBuilder {

    private static final Logger LOG = Logger.getLogger(NomadBackendBuilder.class.getSimpleName());

    private final @Nonnull String         nodeName;
    private final @Nonnull PlatformInfo   platformInfo;
    private final @Nonnull NomadApiClient client;

    public NomadBackendBuilder(@Nonnull String nodeName) {
        this.nodeName     = nodeName;
        this.client       = new NomadApiClient(new NomadApiConfiguration.Builder().build());
        this.platformInfo = tryRetrievePlatformInfoNoThrows(this.client)
                .orElseThrow(() -> new RuntimeException("Failed to contact nomad. Is it running?"));
    }

    @Nonnull
    @Override
    public Optional<PlatformInfo> tryRetrievePlatformInfoNoThrows() {
        return Optional.of(this.platformInfo);
    }

    @Nonnull
    @Override
    public NomadBackend create() throws IOException {
        return new NomadBackend(this.nodeName, this.platformInfo, this.client);
    }


    @Nonnull
    private static Optional<PlatformInfo> tryRetrievePlatformInfoNoThrows(@Nonnull NomadApiClient client) {
        try {
            LOG.info("Collecting platform information from Nomad");
            var stub         = client.getNodesApi().list().getValue().get(0);
            var node         = client.getNodesApi().info(stub.getId()).getValue();
            var cpuFrequency = node.getAttributes().get("cpu.frequency");
            return Optional.ofNullable(cpuFrequency).map(Integer::parseInt).map(PlatformInfo::new);
        } catch (Throwable e) {
            LOG.log(Level.WARNING, "Failed to retrieve (partial) PlatformInfo from Nomad");
            return Optional.empty();
        }

    }
}
