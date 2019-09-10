package de.itd.tracking.winslow;

import de.itd.tracking.winslow.auth.GroupRepository;
import de.itd.tracking.winslow.auth.UserRepository;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;
import de.itd.tracking.winslow.project.ProjectRepository;
import de.itd.tracking.winslow.resource.ResourceManager;

import java.io.IOException;
import java.util.Scanner;

public class Winslow implements Runnable {

    private final Orchestrator               orchestrator;
    private final WorkDirectoryConfiguration configuration;
    private final LockBus                    lockBus;
    private final ResourceManager            resourceManager;
    private final GroupRepository            groupRepository;
    private final UserRepository             userRepository;
    private final PipelineRepository         pipelineRepository;
    private final ProjectRepository          projectRepository;

    public Winslow(Orchestrator orchestrator, WorkDirectoryConfiguration configuration, LockBus lockBus, ResourceManager resourceManager) throws IOException {
        this.orchestrator    = orchestrator;
        this.configuration   = configuration;
        this.lockBus         = lockBus;
        this.resourceManager = resourceManager;

        this.groupRepository    = new GroupRepository();
        this.userRepository     = new UserRepository(groupRepository);
        this.pipelineRepository = new PipelineRepository(lockBus, configuration);
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

    public PipelineRepository getPipelineRepository() {
        return pipelineRepository;
    }
}
