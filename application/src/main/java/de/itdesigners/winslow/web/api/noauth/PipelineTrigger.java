package de.itdesigners.winslow.web.api.noauth;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.pipeline.ImageInfo;
import de.itdesigners.winslow.api.pipeline.ResourceInfo;
import de.itdesigners.winslow.api.project.EnqueueRequest;
import de.itdesigners.winslow.config.Requirements;
import de.itdesigners.winslow.project.Project;
import de.itdesigners.winslow.web.api.ProjectsController;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Optional;
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
    public void triggerPipeline(
            @PathVariable String projectId,
            @RequestParam String secret,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false, defaultValue = "false") boolean runSingle
    ) {
        if (stage != null && stage.trim().length() == 0) {
            stage = null;
        }

        final String fStage = stage;
        getProjectForTokenSecret(projectId, secret, REQUIRED_CAPABILITY_TRIGGER_PIPELINE).ifPresent(project -> {
            var controller = new ProjectsController(winslow);
            var user       = winslow.getUserRepository().getUserOrCreateAuthenticated(project.getOwner()).orElseThrow();

            var stageIndex      = Optional.ofNullable(fStage).flatMap(name -> getStageIndex(project, name)).orElse(0);
            var stageDefinition = project.getPipelineDefinition().getStages().get(stageIndex);

            // default request
            var request = new EnqueueRequest(
                    stageDefinition.getEnvironment(),
                    null,
                    stageIndex,
                    stageDefinition
                            .getImage()
                            .map(i -> new ImageInfo(i.getName(), i.getArgs(), i.getShmSizeMegabytes().orElse(null)))
                            .orElse(null),
                    stageDefinition
                            .getRequirements()
                            .map(r -> new ResourceInfo(
                                         r.getCpu(),
                                         r.getMegabytesOfRam(),
                                         r.getGpu().map(Requirements.Gpu::getCount).orElse(0)
                                 )
                            )
                            .orElse(null),
                    null,
                    "triggered",
                    runSingle,
                    true
            );

            // try to update the request
            var list = controller.getProjectHistory(user, projectId).collect(Collectors.toList());
            for (int n = list.size() - 1; n >= 0; --n) {
                var info = list.get(n);
                if (info.stageDefinition.name.equals(stageDefinition.getName())) {
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
        });

    }

    @Nonnull
    private Optional<Integer> getStageIndex(@Nonnull Project project, @Nonnull String stageName) {
        var stages = project.getPipelineDefinition().getStages();
        for (int i = 0; i < stages.size(); ++i) {
            if (stages.get(i).getName().equals(stageName)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }
}
