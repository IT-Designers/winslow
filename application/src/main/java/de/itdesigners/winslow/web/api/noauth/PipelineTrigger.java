package de.itdesigners.winslow.web.api.noauth;

import de.itdesigners.winslow.Winslow;
import de.itdesigners.winslow.api.pipeline.RangedValue;
import de.itdesigners.winslow.api.pipeline.ResourceInfo;
import de.itdesigners.winslow.api.pipeline.StageWorkerDefinitionInfo;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.api.project.EnqueueRequest;
import de.itdesigners.winslow.config.StageWorkerDefinition;
import de.itdesigners.winslow.project.Project;
import de.itdesigners.winslow.web.ImageInfoConverter;
import de.itdesigners.winslow.web.api.ProjectsController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
            var stageIndex = stageDefIdOpt.flatMap(id -> getStageIndex(project, id)).orElse(0);
            var stageID    = project.getPipelineDefinition().stages().get(stageIndex).id();

            var stageDefinition = project.getPipelineDefinition().stages().get(stageIndex);

            var imageInfo = stageDefinition instanceof StageWorkerDefinition w
                            ? ImageInfoConverter.from(w.image())
                            : null;
            var resourceInfo = stageDefinition instanceof StageWorkerDefinition w
                               ? new ResourceInfo(
                    w.requirements().getCpus().orElse(0),
                    w.requirements().getMegabytesOfRam().orElse(0L),
                    w.requirements().getGpu().getCount()
            )
                               : null;

            // default request
            var rangedEnv              = (Map<String, RangedValue>) null;
            var env                    = stageDefinition.environment();
            var image                  = imageInfo;
            var requiredResources      = resourceInfo;
            var workspaceConfiguration = (WorkspaceConfiguration) null;

            // try to update the request
            var list = controller.getProjectHistoryUnchecked(projectId).toList();
            for (int n = list.size() - 1; n >= 0; --n) {
                var info = list.get(n);
                if (info.stageDefinition().id().equals(stageDefinition.id())) {
                    rangedEnv              = info.rangedValues();
                    workspaceConfiguration = info.workspaceConfiguration();

                    if (info.stageDefinition() instanceof StageWorkerDefinitionInfo workerDefinitionInfo) {
                        env               = workerDefinitionInfo.environment();
                        image             = workerDefinitionInfo.image();
                        requiredResources = new ResourceInfo(
                                workerDefinitionInfo.requiredResources().cpus(),
                                workerDefinitionInfo.requiredResources().megabytesOfRam(),
                                workerDefinitionInfo.requiredResources().gpu().count()
                        );
                    }
                    break;

                }
            }


            controller.enqueueStageToExecuteUnchecked(projectId, new EnqueueRequest(
                    stageID.toString(),
                    env,
                    rangedEnv,
                    image,
                    requiredResources,
                    workspaceConfiguration,
                    "triggered",
                    runSingle,
                    true
            ));
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
