package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.PipelineRepository;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.config.StageWorkerDefinition;
import de.itdesigners.winslow.config.StageXOrGatwayDefinition;
import de.itdesigners.winslow.pipeline.StageId;
import de.itdesigners.winslow.project.ProjectRepository;
import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.JsonLogicException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class XOrGateway extends Gateway {

    private final @Nonnull PipelineRepository       pipelines;
    private final @Nonnull ProjectRepository        projects;
    private final @Nonnull StageXOrGatwayDefinition stageDefinition;
    private                StageId               stageId;

    public XOrGateway(
            @Nonnull PipelineRepository pipelines,
            @Nonnull ProjectRepository projects,
            @Nonnull StageXOrGatwayDefinition stageDefinition,
            @Nonnull StageId stageId) {
        this.pipelines       = pipelines;
        this.projects        = projects;
        this.stageDefinition = stageDefinition;
        this.stageId         = stageId;
    }

    @Override
    public void execute() {
        this.log(Level.INFO, "XOrGateway!");

        var projectHandle   = this.projects.getProject(this.stageId.getProjectId());
        var projectReadOnly = projectHandle.unsafe().orElseThrow();
        // var exclusiveWriteable = projectHandle.exclusive();


        var pipelineHandle   = this.pipelines.getPipeline(this.stageId.getProjectId());
        var pipelineReadOnly = pipelineHandle.unsafe().orElseThrow();

        var thisExecutionGroup = pipelineReadOnly.getActiveExecutionGroups().filter(eg -> eg
                .getId()
                .equals(this.stageId.getExecutionGroupId())).findFirst();

        var rootNode = new Node(thisExecutionGroup.get().getStageDefinition(), thisExecutionGroup.get());
        var graph    = new Graph(pipelineReadOnly, projectReadOnly.getPipelineDefinition(), rootNode);

        var numberOfInvocationsOfMyself = rootNode.getExecutionGroups().size();
        this.log(Level.INFO, "numberOfInvocationsOfMyself: " + numberOfInvocationsOfMyself);

        if (numberOfInvocationsOfMyself == 1) {
            var myInvoker = rootNode.getPreviousNodes().stream().filter(p -> !p.getExecutionGroups().isEmpty())
                                    .findFirst()
                                    .map(p -> p.getExecutionGroups().get(0))
                                    .orElseThrow();

            var result = myInvoker.getStages().findFirst().orElseThrow().getResult();

            final var nextStageDefinitionId = findNextStageDefinitionID(thisExecutionGroup, result);

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

                    // TODO select via condition?
                    this.log(Level.INFO, "next stages: " + thisExecutionGroup.get().getStageDefinition().nextStages());


                    pipeline.enqueueSingleExecution(
                            projectReadOnly
                                    .getPipelineDefinition()
                                    .stages()
                                    .stream()
                                    .filter(stageDefinition1 -> stageDefinition1
                                            .id()
                                            .equals(nextStageDefinitionId))
                                    .findFirst()
                                    .orElseThrow(() -> new IOException("Failed to load")),
                            new WorkspaceConfiguration(),
                            "automatic from " + getClass().getSimpleName(),
                            thisExecutionGroup.get().getId()
                    );

                    lockedPipelineHandle.update(pipeline);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        }
    }

    private UUID findNextStageDefinitionID(Optional<ExecutionGroup> thisExecutionGroup, Map<String, String> result) {
        var  conditions                   = stageDefinition.conditions();

        this.log(Level.INFO, "args: " + Arrays.toString(conditions));
        for (int i = 0; i < conditions.length; i++) {
            this.log(Level.INFO, "index: " + i);
            this.log(Level.INFO, "value: " + conditions[i]);
            var logic = new JsonLogic();
            var data  = new HashMap<String, Object>();
            data.putAll(stageDefinition.environment());
            data.putAll(result);
            try {
                boolean logic_result = (boolean) logic.apply(conditions[i], data);
                this.log(Level.INFO, "logic_result: " + logic_result);
                if (logic_result) {
                    return thisExecutionGroup
                            .get()
                            .getStageDefinition()
                            .nextStages()
                            .get(i);
                }
            } catch (JsonLogicException ex) {
                ex.printStackTrace();
            }
        }
            throw new RuntimeException("Could not calculate ResultIndex");
    }
}
