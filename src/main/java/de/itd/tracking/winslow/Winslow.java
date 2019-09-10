package de.itd.tracking.winslow;

import de.itd.tracking.winslow.auth.GroupRepository;
import de.itd.tracking.winslow.auth.UserRepository;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;
import de.itd.tracking.winslow.project.ProjectRepository;
import de.itd.tracking.winslow.resource.ResourceManager;

import java.io.IOException;
import java.util.Optional;
import java.util.Scanner;

public class Winslow implements Runnable {

    private final Orchestrator                 orchestrator;
    private final ResourceManager              resourceManager;
    private final GroupRepository              groupRepository;
    private final UserRepository               userRepository;
    private final PipelineDefinitionRepository pipelineRepository;
    private final ProjectRepository            projectRepository;

    public Winslow(Orchestrator orchestrator, WorkDirectoryConfiguration configuration, LockBus lockBus, ResourceManager resourceManager) throws IOException {
        this.orchestrator    = orchestrator;
        this.resourceManager = resourceManager;

        this.groupRepository    = new GroupRepository();
        this.userRepository     = new UserRepository(groupRepository);
        this.pipelineRepository = new PipelineDefinitionRepository(lockBus, configuration);
        this.projectRepository  = new ProjectRepository(lockBus, configuration);
    }

    public Orchestrator getOrchestrator() {
        return orchestrator;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public ProjectRepository getProjectRepository() {
        return projectRepository;
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
