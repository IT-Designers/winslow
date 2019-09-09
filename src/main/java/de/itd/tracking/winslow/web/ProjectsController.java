package de.itd.tracking.winslow.web;

import de.itd.tracking.winslow.Submission;
import de.itd.tracking.winslow.Winslow;
import de.itd.tracking.winslow.auth.User;
import de.itd.tracking.winslow.fs.LockException;
import de.itd.tracking.winslow.project.Project;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

@RestController
public class ProjectsController {

    private final Winslow winslow;

    public ProjectsController(Winslow winslow) {
        this.winslow = winslow;
    }

    @GetMapping("/projects")
    public Stream<Project> listProjects(User user) {
        return winslow
                .getProjectRepository()
                .getProjects()
                .flatMap(handle -> handle.unsafe().stream())
                .filter(project -> canUserAccessProject(user, project));
    }

    @PostMapping("/projects")
    public Optional<Project> createProject(User user, @RequestParam("name") String name, @RequestParam("pipeline") String pipelineId) {
        return winslow
                .getPipelineRepository()
                .getPipeline(pipelineId)
                .unsafe()
                .flatMap(pipeline -> winslow
                        .getProjectRepository()
                        .createProject(pipeline, user, project -> project.setName(name)));
    }

    @GetMapping("/projects/{projectId}/history")
    public Stream<Submission.HistoryEntry> getProjectHistory(User user, @PathVariable("projectId") String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .stream()
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getSubmissionUnsafe(project)
                        .stream()
                        .flatMap(Submission::getHistory));
    }

    @GetMapping("/projects/{projectId}/state")
    public Optional<Submission.State> getProjectState(User user, @PathVariable("projectId") String projectId) {
        return winslow
                .getProjectRepository()
                .getProject(projectId)
                .unsafe()
                .filter(project -> canUserAccessProject(user, project))
                .flatMap(project -> winslow
                        .getOrchestrator()
                        .getSubmissionUnsafe(project)
                        .flatMap(Submission::getStateOptional));
    }

    @PostMapping("projects/{projectId}/nextStage/{stageIndex}")
    public void setProjectNextStage(User user, @PathVariable("projectId") String projectId, @PathVariable("stageIndex") int index) {
        var project = winslow.getProjectRepository().getProject(projectId);
        if (project.unsafe().map(p -> canUserAccessProject(user, p)).orElse(false)) {
            project.locked().ifPresent(p -> {
                try {
                    var updated = p.get().map(pget -> {
                        pget.setNextStageIndex(index);
                        pget.setForceProgressOnce(true);
                        return pget;
                    });
                    if (updated.isPresent()) {
                        p.update(updated.get());
                    }
                } catch (LockException | IOException e) {
                    throw new RuntimeException("Failed to set next stage index", e);
                }
            });
        }
    }

    private boolean canUserAccessProject(@Nonnull User user, @Nonnull Project project) {
        return project.getOwner().equals(user.getName()) || user.getGroups().anyMatch(g -> {
            for (String group : project.getGroups()) {
                if (group.equals(g)) {
                    return true;
                }
            }
            return false;
        });
    }
}
