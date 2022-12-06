package de.itdesigners.winslow.web.api.noauth;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.pipeline.ResourceInfo;
import de.itdesigners.winslow.api.pipeline.StageWorkerDefinitionInfo;
import de.itdesigners.winslow.api.project.EnqueueRequest;
import de.itdesigners.winslow.config.StageWorkerDefinition;
import de.itdesigners.winslow.project.Project;
import de.itdesigners.winslow.web.ImageInfoConverter;
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

            var stageIndex = stageDefIdOpt.flatMap(id -> getStageIndex(project, id)).orElse(0);
            var stageID    = project.getPipelineDefinition().stages().get(stageIndex).id();

            var stageDefinition = project.getPipelineDefinition().stages().get(stageIndex);

            var imageInfo = stageDefinition instanceof StageWorkerDefinition w
                            ? ImageInfoConverter.from(w.image())
                            : null;
            var resourceInfo = stageDefinition instanceof StageWorkerDefinition w
                               ? new ResourceInfo(
                    w.requirements().getCpus(),
                    w.requirements().getMegabytesOfRam(),
                    w.requirements().getGpu().getCount()
            )
                               : null;

            // default request
            var request = new EnqueueRequest(
                    stageID.toString(),
                    stageDefinition.environment(),
                    null,
                    imageInfo,
                    resourceInfo,
                    null,
                    "triggered",
                    runSingle,
                    true
            );

            // try to update the request
            var list = controller.getProjectHistory(user, projectId).collect(Collectors.toList());
            for (int n = list.size() - 1; n >= 0; --n) {
                var info = list.get(n);
                if (info.stageDefinition.id().equals(stageDefinition.id())) {


                    request.rangedEnv              = info.rangedValues;

                    if(info.stageDefinition instanceof StageWorkerDefinitionInfo workerDefinitionInfo) {
                        request.env               = Optional
                                .ofNullable(workerDefinitionInfo.environment())
                                .orElseGet(HashMap::new);
                        request.image             = workerDefinitionInfo.image();
                        request.requiredResources = new ResourceInfo(
                                workerDefinitionInfo.requiredResources().cpus(),
                                workerDefinitionInfo.requiredResources().ram(),
                                workerDefinitionInfo.requiredResources().gpu().count()
                        );
                    }
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
        var stages = project.getPipelineDefinition().stages();
        for (int i = 0; i < stages.size(); ++i) {
            if (stages.get(i).id().equals(stageDefId)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }
}
