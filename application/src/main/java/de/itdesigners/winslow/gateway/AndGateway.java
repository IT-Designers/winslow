package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.PipelineDefinitionRepository;
import de.itdesigners.winslow.PipelineRepository;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.config.StageAndGatewayDefinition;
import de.itdesigners.winslow.pipeline.StageId;
import de.itdesigners.winslow.project.ProjectRepository;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.logging.Level;

public class AndGateway extends Gateway {

    private final @Nonnull PipelineDefinitionRepository pipelineDefinitions;
    private final @Nonnull PipelineRepository           pipelines;
    private final @Nonnull ProjectRepository         projects;
    private final @Nonnull StageAndGatewayDefinition stageDefinition;
    private final          StageId                   stageId;

    public AndGateway(
            @Nonnull PipelineDefinitionRepository pipelineDefinitions,
            @Nonnull PipelineRepository pipelines,
            @Nonnull ProjectRepository projects,
            @Nonnull StageAndGatewayDefinition stageDefinition,
            @Nonnull StageId stageId) {
        this.pipelineDefinitions = pipelineDefinitions;
        this.pipelines           = pipelines;
        this.projects            = projects;
        this.stageDefinition     = stageDefinition;
        this.stageId             = stageId;
    }

    @Override
    public void execute() {
        this.log(Level.INFO, "AndGateway!");

        var projectHandle   = this.projects.getProject(this.stageId.getProjectId());
        var projectReadOnly = projectHandle.unsafe().orElseThrow();
        // var exclusiveWriteable = projectHandle.exclusive();


        var pipelineHandle   = this.pipelines.getPipeline(this.stageId.getProjectId());
        var pipelineReadOnly = pipelineHandle.unsafe().orElseThrow();

        var thisExecutionGroup = pipelineReadOnly.getActiveExecutionGroups().filter(eg -> eg
                .getId()
                .equals(this.stageId.getExecutionGroupId())).findFirst();

        var rootNode = new Node(thisExecutionGroup.get().getStageDefinition(), thisExecutionGroup.get());
        var pipelineDef = pipelineDefinitions.getPipelineDefinitionReadonly(projectReadOnly).orElseThrow();
        var graph       = new Graph(pipelineReadOnly, pipelineDef, rootNode);


        var numberOfPrevStageDefinitions = rootNode.getPreviousNodes().size();
        var numberOfInvocationsOfMyself  = rootNode.getExecutionGroups().size();


        this.log(Level.INFO, "numberOfInvocationsOfMyself: " + numberOfInvocationsOfMyself);
        this.log(Level.INFO, "numberOfPrevStageDefinitions: " + numberOfPrevStageDefinitions);

        if (numberOfInvocationsOfMyself == numberOfPrevStageDefinitions) {
            this.log(Level.INFO, "In if!");
            try {
                // TODO: Legacy code
                this.log(Level.SEVERE, "Legacy code starting");
                Thread.sleep(5_000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // enqueue
            // Handle<Pipeline> -> pipeline.Enqueue(next)
            pipelineHandle.exclusive().ifPresent(lockedPipelineHandle -> {

                this.log(Level.INFO, "exclusive pl access");
                try (lockedPipelineHandle) {
                    var pipeline = lockedPipelineHandle.get().orElseThrow(() -> new IOException("Failed to load"));

                    this.log(Level.INFO, "next stages: " + thisExecutionGroup.get().getStageDefinition().nextStages());
                    for (var nextStageDefinitionNames : thisExecutionGroup.get().getStageDefinition().nextStages()) {
                        pipeline.enqueueSingleExecution(
                                pipelineDef
                                        .stages()
                                        .stream()
                                        .filter(stageDefinition1 -> stageDefinition1
                                                .id()
                                                .equals(nextStageDefinitionNames))
                                        .findFirst()
                                        .orElseThrow(() -> new IOException("Failed to load")),
                                new WorkspaceConfiguration(),
                                "automatic from " + getClass().getSimpleName(),
                                thisExecutionGroup.get().getId()
                        );
                    }
                    lockedPipelineHandle.update(pipeline);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        }
    }
}
