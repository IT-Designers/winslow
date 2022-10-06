package de.itdesigners.winslow;

import de.itdesigners.winslow.auth.*;
import de.itdesigners.winslow.cli.FixWorkspacePaths;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.fs.LockBus;
import de.itdesigners.winslow.fs.WorkDirectoryConfiguration;
import de.itdesigners.winslow.node.NodeRepository;
import de.itdesigners.winslow.project.AuthTokenRepository;
import de.itdesigners.winslow.project.LogRepository;
import de.itdesigners.winslow.project.ProjectRepository;
import de.itdesigners.winslow.resource.ResourceManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Winslow implements Runnable {

    private static final Logger LOG = Logger.getLogger(Winslow.class.getSimpleName());

    @Nonnull private final Orchestrator                 orchestrator;
    @Nonnull private final WorkDirectoryConfiguration   configuration;
    @Nonnull private final ResourceManager              resourceManager;
    @Nonnull private final GroupManager                 groupManager;
    @Nonnull private final UserManager                  userManager;
    @Nonnull private final PipelineDefinitionRepository pipelineRepository;
    @Nonnull private final ProjectRepository            projectRepository;
    @Nonnull private final AuthTokenRepository          projectAuthTokenRepository;
    @Nonnull private final NodeRepository               nodeRepository;
    @Nonnull private final SettingsRepository           settingsRepository;

    public Winslow(
            @Nonnull Orchestrator orchestrator,
            @Nonnull WorkDirectoryConfiguration configuration,
            @Nonnull LockBus lockBus,
            @Nonnull ResourceManager resourceManager,
            @Nonnull ProjectRepository projectRepository,
            @Nonnull AuthTokenRepository projectAuthTokenRepository,
            @Nonnull SettingsRepository settingsRepository,
            @Nonnull NodeRepository nodeRepository,
            @Nonnull GroupManager groupManager,
            @Nonnull UserManager userManager) throws IOException {
        this.orchestrator               = orchestrator;
        this.configuration              = configuration;
        this.resourceManager            = resourceManager;
        this.groupManager               = groupManager;
        this.userManager                = userManager;
        this.projectAuthTokenRepository = projectAuthTokenRepository;

        this.pipelineRepository = new PipelineDefinitionRepository(lockBus, configuration);
        this.projectRepository  = projectRepository;
        this.settingsRepository = settingsRepository;
        this.nodeRepository     = nodeRepository;

        for (var userName : Env.getRootUsers()) {
            try {
                userManager.createUserAndGroupIgnoreIfAlreadyExists(userName);
                groupManager.addOrUpdateMembership(Group.SUPER_GROUP_NAME, userName, Role.OWNER);
            } catch (InvalidNameException | NameNotFoundException e) {
                // there is nothing reasonable recovery from this, fail fast, don't hide the error!
                throw new RuntimeException(e);
            }
        }
    }

    public void run() {
        LOG.info("Starting the orchestrator");
        orchestrator.start();

        LOG.info("Serving interactive console");
        var commands = new TreeMap<String, Consumer<ConsoleHandle>>();
        commands.put("exit", ConsoleHandle::stop);
        commands.put("stop", ConsoleHandle::stop);
        commands.put("reload", this::reload);
        commands.put("fix-workspace-paths", this::fixWorkspacePaths);
        commands.put("prune-lost-workspaces", this::pruneLostWorkspaces);

        try (Scanner scanner = new Scanner(System.in)) {
            var handle = new ConsoleHandle(scanner);

            String line;
            while ((line = handle.nextLine()) != null) {
                if (commands.containsKey(line)) {
                    commands.get(line).accept(handle);
                } else if (!line.isEmpty()) {
                    System.out.println("Unknown command, valid commands are: ");
                    commands.keySet().forEach(k -> System.out.println(" - " + k));
                }
            }
        }
    }

    private void reload(@Nonnull ConsoleHandle _console) {
        getProjectRepository()
                .getProjects()
                .map(BaseRepository.Handle::unsafe)
                .flatMap(Optional::stream)
                .filter(project -> orchestrator.getPipeline(project).isEmpty())
                .forEach(project -> {
                    try {
                        orchestrator.createPipeline(project);
                        System.out.println(" - recreated pipeline for " + project.getId());
                    } catch (OrchestratorException e) {
                        System.out.println(" - failed for " + project.getId() + ": " + e.getMessage());
                        e.printStackTrace(System.err);
                    }
                });
    }

    private void pruneLostWorkspaces(@Nonnull ConsoleHandle console) {
        getOrchestrator()
                .getPipelines()
                .getAllPipelines()
                .flatMap(h -> h.unsafe().stream())
                .forEach(pipeline -> {
                    resourceManager.getWorkspace(Path.of(pipeline.getProjectId())).ifPresent(workspace -> {
                        var directories = new TreeMap<String, Path>();
                        try (var list = Files.list(workspace)) {
                            list
                                    .map(p -> p.getFileName().toString())
                                    .filter(p -> p.contains("_"))
                                    .filter(p -> p.split("_", 2)[0].matches("[\\d]+"))
                                    .map(workspace::resolve)
                                    .forEach(w -> resourceManager
                                            .getWorkspacesDirectory()
                                            .map(d -> d.relativize(w))
                                            .ifPresent(relative -> directories.put(relative.toString(), w))
                                    );
                            pipeline
                                    .getActiveAndPastExecutionGroups()
                                    .flatMap(ExecutionGroup::getStages)
                                    .flatMap(s -> s.getWorkspace().stream())
                                    .forEach(directories::remove);
                            directories.keySet().forEach(System.out::println);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (!directories.isEmpty()) {

                            System.out.println(">> Delete? [y/N] ");
                            var result = console.nextLine();

                            if ("y".equals(result)) {
                                directories.values().forEach(directory -> {
                                    try {
                                        orchestrator.forcePurgeWorkspace(
                                                pipeline.getProjectId(),
                                                directory
                                        );
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                            } else {
                                System.out.println("Aborted!");
                            }
                        }
                    });
                });
        System.out.println("Done.");
    }

    private void fixWorkspacePaths(@Nonnull ConsoleHandle _console) {
        new FixWorkspacePaths()
                .withProjectIds(this.projectRepository.getProjectIds().collect(Collectors.toList()))
                .searchForProjectsWithFixableWorkspaces(projectRepository, orchestrator)
                .tryFixProjectsWithFixableWorkspacePaths(orchestrator)
                .printResults(System.out);
    }

    @Nonnull
    public Orchestrator getOrchestrator() {
        return orchestrator;
    }

    @Nonnull
    public WorkDirectoryConfiguration getWorkDirectoryConfiguration() {
        return configuration;
    }

    @Nonnull
    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    @Nonnull
    public ProjectRepository getProjectRepository() {
        return projectRepository;
    }


    @Nonnull
    public AuthTokenRepository getProjectAuthTokenRepository() {
        return projectAuthTokenRepository;
    }

    @Nonnull
    public NodeRepository getNodeRepository() {
        return nodeRepository;
    }

    @Nonnull
    public RunInfoRepository getRunInfoRepository() {
        return getOrchestrator().getRunInfoRepository();
    }

    @Nonnull
    public LogRepository getLogRepository() {
        return getOrchestrator().getLogRepository();
    }

    @Nonnull
    public UserManager getUserManager() {
        return this.userManager;
    }

    @Nonnull
    public GroupManager getGroupManager() {
        return groupManager;
    }

    @Nonnull
    public PipelineDefinitionRepository getPipelineRepository() {
        return pipelineRepository;
    }

    @Nonnull
    public SettingsRepository getSettingsRepository() {
        return settingsRepository;
    }

    private static class ConsoleHandle {
        private final @Nonnull Scanner scanner;
        private                boolean keepRunning = true;

        private ConsoleHandle(@Nonnull Scanner scanner) {
            this.scanner = scanner;
        }

        public @Nullable
        String nextLine() {
            if (keepRunning) {
                return this.scanner.nextLine();
            } else {
                return null;
            }
        }

        public void stop() {
            this.keepRunning = false;
        }
    }
}
