package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.PipelineRepository;
import de.itdesigners.winslow.api.pipeline.WorkspaceConfiguration;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.Stage;
import de.itdesigners.winslow.pipeline.StageId;
import de.itdesigners.winslow.project.ProjectRepository;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class AndGateway extends Gateway {

    private final @Nonnull PipelineRepository pipelines;
    private final @Nonnull ProjectRepository  projects;
    private final @Nonnull StageDefinition    stageDefinition;
    private                StageId            stageId;

    public AndGateway(
            @Nonnull PipelineRepository pipelines,
            @Nonnull ProjectRepository projects,
            @Nonnull StageDefinition stageDefinition,
            @Nonnull StageId stageId) {
        this.pipelines       = pipelines;
        this.projects        = projects;
        this.stageDefinition = stageDefinition;
        this.stageId         = stageId;
    }

    @Override
    public void execute() {
        this.log(Level.INFO, "AndGateway!");

        var projectHandle   = this.projects.getProject(this.stageId.getProjectId());
        var projectReadOnly = projectHandle.unsafe().orElseThrow();
        // var exclusiveWriteable = projectHandle.exclusive();


        var pipelineHandle   = this.pipelines.getPipeline(this.stageId.getProjectId());
        var pipelineReadOnly = pipelineHandle.unsafe().orElseThrow();

        // pipelineReadOnly.getActiveAndPastExecutionGroups().filter(eg -> eg.getId().equals(stageId)) ...

        // here Samuel, have fun
        /*
        
        class Node {
            List<Node> prev;
            List<Node> next;
            List<EG> egs;
                                        // String stageName;
            StageDefinition definition; // definition.getName() und definition.getNext()
        }

        class Graph {
            List<Node> nodes

            // optional for performance
            Map<EGId, EG> cacheExecutionGroupForExGrId;         // und / oder
            Map<EGId, Node> cacheNodeForExecutionGroupId;       // und / oder
            Map<String, Node> cacheNodeForStageDefinitionName;  // und / oder
        }

        */

        var thisExecutionGroup = pipelineReadOnly.getActiveExecutionGroups().filter(eg -> eg
                .getId()
                .equals(this.stageId.getExecutionGroupId())).findFirst();

        var rootNode = new Node(thisExecutionGroup.get().getStageDefinition(), thisExecutionGroup.get());
        var graph    = new Graph(pipelineReadOnly, projectReadOnly.getPipelineDefinition(), rootNode);

        var numberOfPrevStageDefinitions = rootNode.getPreviousNodes().size();
        var numberOfInvocationsOfMyself  = rootNode.getExecutionGroups().size();

        if (numberOfInvocationsOfMyself == numberOfPrevStageDefinitions) {
            // enqueue
            // Handle<Pipeline> -> pipeline.Enqueue(next)
            pipelineHandle.exclusive().ifPresent(lockedPipelineHandle -> {
                try (lockedPipelineHandle) {
                    var pipeline = lockedPipelineHandle.get().orElseThrow(() -> new IOException("Failed to load"));

                    for (var nextStageDefinitionNames : thisExecutionGroup.get().getStageDefinition().getNextStages()) {
                        pipeline.enqueueSingleExecution(
                                projectReadOnly
                                        .getPipelineDefinition()
                                        .getStages()
                                        .stream()
                                        .filter(stageDefinition1 -> stageDefinition1
                                                .getName()
                                                .equals(nextStageDefinitionNames))
                                        .findFirst()
                                        .orElseThrow(() -> new IOException("Failed to load")),
                                new WorkspaceConfiguration(),
                                "automatic from " + getClass().getSimpleName(),
                                thisExecutionGroup.get().getId()
                        );
                    }

                } catch (Throwable t) {
                    t.printStackTrace();
                }
            });
        }


        /*
        laden der Pipeline pipelines.getPipeline(stageId.getProjectId())
            pl = 3 listen der executiongroups mit parentid

        wir müssen unsere EG in der Pipeline finden (ueg), hint: müsste in "active EGs sein"
            where ueg.id == stageId.getExecutionGroupId()
            ueg => EG für dieses Gateway

        baue graph mit executiongroups bei die stageId der jeweiligen EG als parentId bei uns hinterlegt ist (verkettet)
            where recursive (pl.EG.stageId == ueg.parentId)     ?
            hint: Map<ExecutionGroupId, EG> for more performance (cache, optional)

        lade PipelineDefinition via ProjectRepository.getProject(stageId.getProject()).getPipelineDefinition()
            projectRepo -> Project -> PipelineDefinition
            graph anhand von StageDefinitions "vervollständigen" (stageDefinition.next) / leere Listen von EGs

        Fehlende Eingänge suchen
            zbsp Node.prevNodes mit leeren EG listen

         */
    }
}
