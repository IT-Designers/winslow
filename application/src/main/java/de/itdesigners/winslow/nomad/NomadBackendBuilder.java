package de.itdesigners.winslow.nomad;

import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadApiConfiguration;
import de.itdesigners.winslow.BackendBuilder;
import de.itdesigners.winslow.node.PlatformInfo;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Logger;

public class NomadBackendBuilder implements BackendBuilder {

    private static final Logger LOG = Logger.getLogger(NomadBackendBuilder.class.getSimpleName());

    private final @Nonnull String         nodeName;
    private final @Nonnull PlatformInfo   platformInfo;
    private final @Nonnull NomadApiClient client;

    public NomadBackendBuilder(@Nonnull String nodeName, @Nonnull PlatformInfo platformInfo) {
        this.nodeName     = nodeName;
        this.platformInfo = platformInfo;
        this.client       = new NomadApiClient(new NomadApiConfiguration.Builder().build());
    }

    @Nonnull
    @Override
    public NomadBackend create() throws IOException {
        return new NomadBackend(this.nodeName, this.platformInfo, this.client);
    }
}
