package de.itd.tracking.winslow.nomad;

import com.hashicorp.nomad.javasdk.NomadApiClient;
import de.itd.tracking.winslow.node.GpuInfo;
import de.itd.tracking.winslow.node.Node;
import de.itd.tracking.winslow.node.NodeInfo;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class NomadGpuDetectorNodeWrapper implements Node {

    @Nonnull private final Node           node;
    @Nonnull private final List<GpuInfo>  gpuInfo;

    public NomadGpuDetectorNodeWrapper(@Nonnull Node node, @Nonnull NomadApiClient nomad) throws IOException {
        this.node    = node;
        this.gpuInfo = NomadBackend.listGpus(nomad);
    }

    @Nonnull
    @Override
    public String getName() {
        return this.node.getName();
    }

    @Nonnull
    @Override
    public NodeInfo loadInfo() throws IOException {
        var info = this.node.loadInfo();
        return new NodeInfo(
                info.getName(),
                info.getCpuInfo(),
                info.getMemInfo(),
                info.getNetInfo(),
                info.getDiskInfo(),
                this.gpuInfo
        );
    }
}
