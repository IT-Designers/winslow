package de.itd.tracking.winslow;

import de.itd.tracking.winslow.auth.GroupRepository;
import de.itd.tracking.winslow.auth.UserRepository;
import de.itd.tracking.winslow.fs.LockBus;
import de.itd.tracking.winslow.fs.LockException;
import de.itd.tracking.winslow.fs.WorkDirectoryConfiguration;
import de.itd.tracking.winslow.project.Project;
import de.itd.tracking.winslow.project.ProjectRepository;
import de.itd.tracking.winslow.resource.PathConfiguration;
import de.itd.tracking.winslow.resource.ResourceManager;

import java.io.IOException;

public class Winslow implements Runnable {

    private final Orchestrator               orchestrator;
    private final WorkDirectoryConfiguration configuration;
    private final LockBus                    lockBus;
    private final ResourceManager            resourceManager;
    private final GroupRepository            groupRepository;
    private final UserRepository             userRepository;
    private final PipelineRepository         pipelineRepository;
    private final ProjectRepository          projectRepository;

    public Winslow(Orchestrator orchestrator, WorkDirectoryConfiguration configuration) throws IOException {
        this.orchestrator  = orchestrator;
        this.configuration = configuration;

        this.lockBus         = new LockBus(configuration.getEventsDirectory());
        this.resourceManager = new ResourceManager(configuration.getPath(), new PathConfiguration());

        this.groupRepository    = new GroupRepository();
        this.userRepository     = new UserRepository(groupRepository);
        this.pipelineRepository = new PipelineRepository(lockBus, configuration);
        this.projectRepository  = new ProjectRepository(lockBus, configuration);
    }


    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public ProjectRepository getProjectRepository() {
        return projectRepository;
    }

    public void run() {
        try {
            while (true) {
                getProjectRepository()
                        .getProjects()
                        .filter(handle -> {
                            var loaded = handle.unsafe();
                            return loaded.isPresent() && orchestrator.canProgressLockFree(loaded.get());
                        })
                        .flatMap(handle -> handle.locked().stream())
                        .peek(this::tryMakeProgress)
                        .forEach(LockedContainer::close);

                getProjectRepository()
                        .getProjects()
                        .filter(handle -> {
                            var loaded = handle.unsafe();
                            return loaded.isPresent() && orchestrator.hasPendingChanges(loaded.get());
                        })
                        .flatMap(handle -> handle.unsafe().stream())
                        .forEach(orchestrator::updateInternalState);

                synchronized (this) {
                    this.wait(10_000);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted unexpectedly", e);
        }
    }

    protected void tryMakeProgress(LockedContainer<Project> container) {
        try {
            var containerProject = container.get();
            if (containerProject.isPresent()) {
                var project = containerProject.get();
                orchestrator.startNext(project, new Environment(configuration, resourceManager));
                container.update(project);
            }
        } catch (LockException | IOException e) {
            e.printStackTrace();
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
