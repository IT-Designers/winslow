package de.itdesigners.winslow;

import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadApiConfiguration;
import de.itdesigners.winslow.api.node.NodeInfo;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.LockException;
import de.itdesigners.winslow.fs.NfsWorkDirectory;
import de.itdesigners.winslow.node.Node;
import de.itdesigners.winslow.node.NodeInfoUpdater;
import de.itdesigners.winslow.node.unix.UnixNode;
import de.itdesigners.winslow.nomad.NomadBackend;
import de.itdesigners.winslow.nomad.NomadGpuDetectorNodeWrapper;
import de.itdesigners.winslow.project.LogRepository;
import de.itdesigners.winslow.project.ProjectRepository;
import de.itdesigners.winslow.resource.PathConfiguration;
import de.itdesigners.winslow.resource.ResourceManager;
import de.itdesigners.winslow.web.WebApi;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOG = Logger.getLogger(Main.class.getSimpleName());

    public static void main(String[] args) throws UnknownHostException {
        configureLogger();
        final String workDirectory = Env.getWorkDirectory();
        final String nodeName      = Env.getNodeName();

        System.out.println();
        System.out.println("           ____               ");
        System.out.println("         /       \\            ");
        System.out.println("        /   ------\\           ");
        System.out.println("       /___/      |            ");
        System.out.println("       /\\   ^   ^ |                   _           _               ");
        System.out.println("       \\/     ;   /                  (_)         | |              ");
        System.out.println("        /\\   __  /          __      ___ _ __  ___| | _____      __");
        System.out.println("       /   -----            \\ \\ /\\ / / | '_ \\/ __| |/ _ \\ \\ /\\ / /");
        System.out.println("      /\\    /\\  \\            \\ V  V /| | | | \\__ \\ | (_) \\ V  V / ");
        System.out.println("     /--\\   \\/   \\            \\_/\\_/ |_|_| |_|___/_|\\___/ \\_/\\_/  ");
        System.out.println("                                                     v0.0.0                  ");
        System.out.println();
        System.out.println();
        System.out.println("             node name = " + nodeName);
        System.out.println("        work-directory = " + workDirectory);
        System.out.println("             log-level = INFO");
        System.out.println("                  mode = STANDALONE");
        System.out.println();
        System.out.println();

        WebApi.Context webApi = null;

        try {
            LOG.info("Loading NFS configuration for work-directory");
            NfsWorkDirectory config = NfsWorkDirectory.loadFromCurrentConfiguration(Path.of(workDirectory));

            LOG.info("Preparing environment");
            var lockBus         = new LockBus(nodeName, config.getEventsDirectory());
            var resourceManager = new ResourceManager(config.getPath(), new PathConfiguration());
            var environment     = new Environment(config, resourceManager);
            var logs            = new LogRepository(lockBus, config);
            var projects        = new ProjectRepository(lockBus, config);
            var settings        = new SettingsRepository(lockBus, config);

            LOG.info("Preparing the orchestrator");
            var repository      = new PipelineRepository(lockBus, config);
            var attributes      = new RunInfoRepository(lockBus, config);
            var nomadClient     = new NomadApiClient(new NomadApiConfiguration.Builder().build());
            var node            = getNode(nodeName, nomadClient);
            var resourceMonitor = new ResourceAllocationMonitor(toResourceSet(node.loadInfo()));
            var backend         = new NomadBackend(node.getPlatformInfo(), nomadClient);

            // TODO
            NodeInfoUpdater.spawn(config.getNodesDirectory(), node);

            var orchestrator = new Orchestrator(
                    lockBus,
                    environment,
                    backend,
                    projects,
                    repository,
                    attributes,
                    logs,
                    settings,
                    nodeName,
                    resourceMonitor,
                    !Env.isNoStageExecutionSet()
            );

            if (Env.isNoStageExecutionSet()) {
                LOG.info("Stage execution is disabled, as requested by ENV");
            }

            LOG.info("Assembling Winslow");
            var winslow = new Winslow(nodeName, orchestrator, config, lockBus, resourceManager, projects, settings);


            if (!Env.isNoWebApiSet()) {
                LOG.info("Starting WebApi");
                webApi = WebApi.start(winslow);
            }

            tryFixMissingPipelinesOfProjects(orchestrator, projects);

            LOG.info("Letting Winslow run freely");
            try (orchestrator) {
                winslow.run();
            }
        } catch (IOException | LockException e) {
            e.printStackTrace();
        } finally {
            if (webApi != null) {
                webApi.stop();
            }
        }
    }

    @Nonnull
    private static Node getNode(@Nonnull String nodeName, @Nonnull NomadApiClient nomadClient) throws IOException {
        // TODO
        var unixNode = new UnixNode(nodeName);
        if (Env.isNoGpuUsageSet()) {
            return unixNode;
        } else {
            return new NomadGpuDetectorNodeWrapper(unixNode, nomadClient);
        }
    }

    @Nonnull
    private static ResourceAllocationMonitor.Set<Long> toResourceSet(@Nonnull NodeInfo info) {
        return new ResourceAllocationMonitor.Set<Long>()
                .with(ResourceAllocationMonitor.StandardResources.CPU, (long) info.getCpuInfo().getUtilization().size())
                .with(ResourceAllocationMonitor.StandardResources.RAM, info.getMemInfo().getMemoryTotal())
                .with(ResourceAllocationMonitor.StandardResources.GPU, (long) info.getGpuInfo().size());
    }

    private static void tryFixMissingPipelinesOfProjects(
            @Nonnull Orchestrator orchestrator,
            @Nonnull ProjectRepository projects) {
        projects.getProjects().map(BaseRepository.Handle::unsafe).flatMap(Optional::stream).forEach(project -> {
            var pipeline = orchestrator.getPipeline(project);
            if (pipeline.isEmpty()) {
                LOG.warning("Found Project without Pipeline, trying to create: " + project.getId() + "... ");
                try {
                    orchestrator.createPipeline(project);
                    LOG.warning("Found Project without Pipeline, trying to create: " + project.getId() + "... done");
                } catch (OrchestratorException e) {
                    LOG.log(
                            Level.SEVERE,
                            "Found Project without Pipeline, trying to create: " + project.getId() + "... failed",
                            e
                    );
                }
            }
        });
    }

    private static void configureLogger() {
        // SpringBoot is using SLF4... so setup the bridge early
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }
}
