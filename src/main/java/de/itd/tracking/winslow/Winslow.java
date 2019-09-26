package de.itd.tracking.winslow;

import de.itd.tracking.winslow.auth.GroupRepository;
import de.itd.tracking.winslow.auth.UserRepository;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;
import de.itd.tracking.winslow.node.NodeParser;
import de.itd.tracking.winslow.node.UnixNodeInfoUpdater;
import de.itd.tracking.winslow.project.ProjectRepository;
import de.itd.tracking.winslow.resource.ResourceManager;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;
import java.util.Scanner;

public class Winslow implements Runnable {

    @Nonnull private final Orchestrator                 orchestrator;
    @Nonnull private final ResourceManager              resourceManager;
    @Nonnull private final GroupRepository              groupRepository;
    @Nonnull private final UserRepository               userRepository;
    @Nonnull private final PipelineDefinitionRepository pipelineRepository;
    @Nonnull private final ProjectRepository            projectRepository;
    @Nonnull private final NodeParser                   nodeParser;

    public Winslow(@Nonnull Orchestrator orchestrator, WorkDirectoryConfiguration configuration, LockBus lockBus, ResourceManager resourceManager) throws IOException {
        this.orchestrator    = orchestrator;
        this.resourceManager = resourceManager;

        this.groupRepository    = new GroupRepository();
        this.userRepository     = new UserRepository(groupRepository);
        this.pipelineRepository = new PipelineDefinitionRepository(lockBus, configuration);
        this.projectRepository  = new ProjectRepository(lockBus, configuration);
        this.nodeParser         = new NodeParser(configuration.getNodesDirectory());

        // TODO
        UnixNodeInfoUpdater.spawn("node0", configuration.getNodesDirectory());
    }

    @Nonnull
    public Orchestrator getOrchestrator() {
        return orchestrator;
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
    public NodeParser getNodeParser() {
        return nodeParser;
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
                            .filter(project -> orchestrator.getPipelineOmitExceptions(project).isEmpty())
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

    public UserRepository getUserRepository() {
        return this.userRepository;
    }

    public GroupRepository getGroupRepository() {
        return groupRepository;
    }

    public PipelineDefinitionRepository getPipelineRepository() {
        return pipelineRepository;
    }
}
