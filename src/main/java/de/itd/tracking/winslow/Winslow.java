package de.itd.tracking.winslow;

import de.itd.tracking.winslow.auth.GroupRepository;
import de.itd.tracking.winslow.auth.UserRepository;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;
import de.itd.tracking.winslow.node.NodeRepository;
import de.itd.tracking.winslow.project.LogRepository;
import de.itd.tracking.winslow.project.ProjectRepository;
import de.itd.tracking.winslow.resource.ResourceManager;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;
import java.util.Scanner;

public class Winslow implements Runnable {

    @Nonnull private final Orchestrator                 orchestrator;
    @Nonnull private final WorkDirectoryConfiguration   configuration;
    @Nonnull private final ResourceManager              resourceManager;
    @Nonnull private final GroupRepository              groupRepository;
    @Nonnull private final UserRepository               userRepository;
    @Nonnull private final PipelineDefinitionRepository pipelineRepository;
    @Nonnull private final ProjectRepository            projectRepository;
    @Nonnull private final NodeRepository               nodeRepository;
    @Nonnull private final SettingsRepository           settingsRepository;

    public Winslow(
            @Nonnull String nodeName,
            @Nonnull Orchestrator orchestrator,
            @Nonnull WorkDirectoryConfiguration configuration,
            @Nonnull LockBus lockBus,
            @Nonnull ResourceManager resourceManager,
            @Nonnull ProjectRepository projectRepository) throws IOException {
        this.orchestrator    = orchestrator;
        this.configuration   = configuration;
        this.resourceManager = resourceManager;

        this.groupRepository    = new GroupRepository();
        this.userRepository     = new UserRepository(groupRepository);
        this.pipelineRepository = new PipelineDefinitionRepository(lockBus, configuration);
        this.projectRepository  = projectRepository;
        this.nodeRepository     = new NodeRepository(lockBus, configuration);
        this.settingsRepository = new SettingsRepository(lockBus, configuration);
    }

    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            String line;
            while ((line = scanner.nextLine()) != null) {
                if ("exit".equals(line) || "stop".equals(line)) {
                    break;
                } else if ("reload".equals(line)) {
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

                } else if (!line.isEmpty()) {
                    System.out.println("Unknown command");
                }
            }
        }
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
    public UserRepository getUserRepository() {
        return this.userRepository;
    }

    @Nonnull
    public GroupRepository getGroupRepository() {
        return groupRepository;
    }

    @Nonnull
    public PipelineDefinitionRepository getPipelineRepository() {
        return pipelineRepository;
    }

    @Nonnull
    public SettingsRepository getSettingsRepository() {
        return settingsRepository;
    }
}
