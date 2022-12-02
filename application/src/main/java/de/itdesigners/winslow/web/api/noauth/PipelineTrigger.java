package de.itdesigners.winslow.web.api.noauth;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.pipeline.ImageInfo;
import de.itdesigners.winslow.api.pipeline.ResourceInfo;
import de.itdesigners.winslow.api.project.EnqueueRequest;
import de.itdesigners.winslow.project.Project;
import de.itdesigners.winslow.web.api.ProjectsController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class PipelineTrigger {

    public static final String REQUIRED_CAPABILITY_TRIGGER_PIPELINE = "trigger-pipeline";

    private final @Nonnull Winslow winslow;

    public PipelineTrigger(@Nonnull Winslow winslow) {
        this.winslow = winslow;
    }

    private Optional<Project> getProjectForTokenSecret(
            @Nonnull String projectId,
            @Nonnull String secret,
            @Nonnull String capability) {
        return winslow
                .getProjectAuthTokenRepository()
                .getAuthTokens(projectId)
                .unsafe()
                .flatMap(a -> a.getTokenForSecret(secret))
                .filter(t -> t.hasCapability(capability))
                .flatMap(t -> winslow.getProjectRepository().getProject(projectId).unsafe());
    }

    @RequestMapping(value = {"/trigger/{projectId}"}, method = RequestMethod.GET)
    public ResponseEntity<Void> triggerPipeline(
            @PathVariable String projectId,
            @RequestParam String secret,
            @RequestParam(required = false) String stageDefId,
            @RequestParam(required = false, defaultValue = "false") boolean runSingle
    ) {
        Optional<UUID> stageDefIdOpt = Optional.ofNullable(stageDefId).map(UUID::fromString);

        var result = getProjectForTokenSecret(projectId, secret, REQUIRED_CAPABILITY_TRIGGER_PIPELINE).map(project -> {
            var controller = new ProjectsController(winslow);
            var user       = winslow.getUserManager().getUserOrCreateAuthenticated(project.getOwner()).orElseThrow();

            var stageIndex      = stageDefIdOpt.flatMap(id -> getStageIndex(project, id)).orElse(0);
            var stageDefinition = project.getPipelineDefinition().getStages().get(stageIndex);

            // default request
            var request = new EnqueueRequest(
                    stageDefinition.getEnvironment(),
                    null,
                    stageIndex,
                    new ImageInfo(
                            stageDefinition.getImage().getName(),
                            stageDefinition.getImage().getArgs(),
                            stageDefinition.getImage().getShmSizeMegabytes()
                    ),
                    new ResourceInfo(
                            stageDefinition.getRequirements().getCpu(),
                            stageDefinition.getRequirements().getMegabytesOfRam(),
                            stageDefinition.getRequirements().getGpu().getCount()
                    ),
                    null,
                    "triggered",
                    runSingle,
                    true
            );

            // try to update the request
            var list = controller.getProjectHistory(user, projectId).collect(Collectors.toList());
            for (int n = list.size() - 1; n >= 0; --n) {
                var info = list.get(n);
                if (info.stageDefinition.id.equals(stageDefinition.getId())) {
                    request.env                    = Optional
                            .ofNullable(info.stageDefinition.env)
                            .orElseGet(HashMap::new);
                    request.rangedEnv              = info.rangedValues;
                    request.image                  = info.stageDefinition.image;
                    request.requiredResources      = info.stageDefinition.requiredResources;
                    request.workspaceConfiguration = info.workspaceConfiguration;
                    break;
                }
            }


            controller.enqueueStageToExecute(user, projectId, request);
            return project;
        });

        if (result.isPresent()) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }

    }

    @Nonnull
    private Optional<Integer> getStageIndex(@Nonnull Project project, @Nonnull UUID stageDefId) {
        var stages = project.getPipelineDefinition().getStages();
        for (int i = 0; i < stages.size(); ++i) {
            if (stages.get(i).getId().equals(stageDefId)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }
}
