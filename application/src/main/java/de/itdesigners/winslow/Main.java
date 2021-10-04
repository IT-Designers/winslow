package de.itdesigners.winslow;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.NomadApiConfiguration;
import de.itdesigners.winslow.api.Build;
import de.itdesigners.winslow.api.node.NodeInfo;
import de.itdesigners.winslow.auth.GroupRepository;
import de.itdesigners.winslow.auth.UserRepository;
import de.itdesigners.winslow.fs.*;
import de.itdesigners.winslow.node.Node;
import de.itdesigners.winslow.node.NodeInfoUpdater;
import de.itdesigners.winslow.node.NodeRepository;
import de.itdesigners.winslow.node.PlatformInfo;
import de.itdesigners.winslow.node.unix.UnixNode;
import de.itdesigners.winslow.nomad.NomadBackend;
import de.itdesigners.winslow.project.AuthTokenRepository;
import de.itdesigners.winslow.project.LogRepository;
import de.itdesigners.winslow.project.ProjectRepository;
import de.itdesigners.winslow.resource.PathConfiguration;
import de.itdesigners.winslow.resource.ResourceManager;
import de.itdesigners.winslow.web.WebApi;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
        final String storageType   = Env.getStorageType();
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
        System.out.println("          storage-type = " + storageType);
        System.out.println("             log-level = INFO");
        System.out.println("               version = " + Build.DATE + "@" + Build.COMMIT_HASH_SHORT);
        System.out.println();
        System.out.println();

        WebApi.Context webApi = null;

        try {
            LOG.info("Loading configuration for work-directory");
            WorkDirectoryConfiguration config = getWorkDirectoryConfiguration(workDirectory, storageType);

            LOG.info("Preparing environment");
            var lockBus         = new LockBus(nodeName, config.getEventsDirectory());
            var resourceManager = new ResourceManager(config.getPath(), new PathConfiguration());
            var environment     = new Environment(config, resourceManager);
            var logs            = new LogRepository(lockBus, config);
            var projects        = new ProjectRepository(lockBus, config);
            var tokens          = new AuthTokenRepository(lockBus, config);
            var settings        = new SettingsRepository(lockBus, config);
            var nodes           = new NodeRepository(lockBus, config);
            var groupRepository = new GroupRepository();
            var userRepository  = new UserRepository(groupRepository);

            LOG.info("Preparing the orchestrator");
            var repository      = new PipelineRepository(lockBus, config);
            var attributes      = new RunInfoRepository(lockBus, config);
            var nomadClient     = new NomadApiClient(new NomadApiConfiguration.Builder().build());
            var resourceMonitor = new ResourceAllocationMonitor();
            var node = getNode(
                    nodeName,
                    tryRetrieveNomadPlatformInfoNoThrows(nomadClient).orElse(null),
                    resourceMonitor
            );

            var backend = new NomadBackend(nodeName, node.getPlatformInfo(), nomadClient);
            var updater = NodeInfoUpdater.spawn(nodes, node);
            resourceMonitor.setAvailableResources(toResourceSet(node.loadInfo()));
            resourceMonitor.addChangeListener(updater::updateNoThrows);

            var orchestrator = new Orchestrator(
                    lockBus,
                    environment,
                    backend,
                    projects,
                    repository,
                    attributes,
                    logs,
                    settings,
                    userRepository,
                    nodes,
                    nodeName,
                    resourceMonitor,
                    !Env.isNoStageExecutionSet()
            );

            if (Env.isNoStageExecutionSet()) {
                LOG.info("Stage execution is disabled, as requested by ENV");
            }

            LOG.info("Assembling Winslow");
            var winslow = new Winslow(
                    orchestrator,
                    config,
                    lockBus,
                    resourceManager,
                    projects,
                    tokens,
                    settings,
                    nodes,
                    groupRepository,
                    userRepository
            );


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
    private static WorkDirectoryConfiguration getWorkDirectoryConfiguration(
            String workDirectory,
            String storageType) throws IOException {
        switch (storageType.toLowerCase()) {
            case "nfs":
                return NfsWorkDirectory.loadFromCurrentConfiguration(Path.of(workDirectory));
            case "bind":
                var workDir = Path.of(workDirectory);
                var storDir = Env.getStoragePath().map(Path::of).orElse(workDir);
                return new BindWorkspaceDirectory(workDir, storDir);
            default:
                System.err.println("Invalid storage type: " + storageType.toLowerCase());
                System.exit(1);
                throw new IOException("Invalid storage type: " + storageType.toLowerCase());
        }
    }

    @Nonnull
    private static Optional<PlatformInfo> tryRetrieveNomadPlatformInfoNoThrows(@Nonnull NomadApiClient client) {
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

    @Nonnull
    private static Node getNode(
            @Nonnull String nodeName,
            @Nullable PlatformInfo partialPlatformInfo,
            @Nonnull ResourceAllocationMonitor monitor) throws IOException {
        // TODO
        return new UnixNode(nodeName, partialPlatformInfo, monitor);
    }

    @Nonnull
    private static ResourceAllocationMonitor.ResourceSet<Long> toResourceSet(@Nonnull NodeInfo info) {
        return new ResourceAllocationMonitor.ResourceSet<Long>()
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

        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            var           root    = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            root.setLevel(ch.qos.logback.classic.Level.INFO);
            root.detachAndStopAllAppenders();

            // from spring-boot, simplified default.xml
            PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
            logEncoder.setContext(context);
            logEncoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} -%5p --- [%15.15t] %-40.40logger{39} : %m%n%ex");
            logEncoder.start();

            var appender = new ConsoleAppender<ILoggingEvent>();
            appender.setContext(context);
            appender.setEncoder(logEncoder);
            appender.start();

            root.addAppender(appender);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }
}
