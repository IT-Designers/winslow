package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.pipeline.ExecutionGroupId;
import de.itdesigners.winslow.pipeline.Pipeline;

import javax.annotation.Nonnull;
import java.util.*;

public class Graph {

    private final @Nonnull List<Node>                            nodes;
    private final @Nonnull Map<ExecutionGroupId, ExecutionGroup> cachedExecutionGroup;
    private final @Nonnull Map<String, StageDefinition>          cachedStageDefinition;

    public Graph(@Nonnull Pipeline pipelineReadOnly, @Nonnull PipelineDefinition pipelineDefinition, @Nonnull Node rootNode) {
        this.nodes                 = new ArrayList<>();
        this.cachedExecutionGroup  = new HashMap<>();
        this.cachedStageDefinition = new HashMap<>();

        pipelineReadOnly.getActiveAndPastExecutionGroups().forEach(executionGroup -> {
            cachedExecutionGroup.put(executionGroup.getId(), executionGroup);
        });
        pipelineReadOnly.getActiveExecutionGroups().forEach(executionGroup -> {
            cachedExecutionGroup.put(executionGroup.getId(), executionGroup);
        });

        pipelineDefinition.getStages().forEach(s -> cachedStageDefinition.put(s.getName(), s));

        addNode(rootNode);
        developNodeBackwards(rootNode);
        developNodeForwardsForAllNodes(pipelineDefinition);
    }

    private void developNodeForwardsForAllNodes(@Nonnull PipelineDefinition pipelineDefinition) {
        for (var i = 0; i < nodes.size(); i++) {
            // as the node is developed forwards, new nodes are added to the nodes-list
            // which means nodes.size() gets bigger, thus this for-loop will iterate over them...?
            developNodeForwards(nodes.get(i));
        }
    }

    /**
     * @return the nodes
     */
    @Nonnull
    public List<Node> getNodes() {
        return nodes;
    }

    /**
     * @param node the node to add
     */
    public void addNode(@Nonnull Node node) {
        nodes.add(node);
    }

    public void developNodeBackwards(@Nonnull Node node) {
        // find all StageDefinitions which have us/node.getStageDefinition().getName() as .getNext()
        //    --> these are prev-nodes
        //
        // for (var def : this.cachedStageDefinition.values()) {
        //     if (Objects.equals(def.getName(), node.getStageDefinition().getNextStage().orElse(null))) {
        //         // linking
        //     }
        // }
        //
        // next: for each EG
        //           find its node (it should exist)
        //                  where node.getStageDefinition().getName == EG.getStageDefinition().getName()
        //           add EG to the node

        node
                .getExecutionGroups()
                .stream()
                .flatMap(group -> group.getParentId().stream())
                .flatMap(parentId -> getCachedExecutionGroupForId(parentId).stream())
                .forEach(nextExecutionGroup -> {
                    var prevNode = new Node(
                            nextExecutionGroup.getStageDefinition(),
                            nextExecutionGroup,
                            null,
                            node
                    );
                    addNode(prevNode);
                    node.addPreviousNode(prevNode);
                    developNodeBackwards(prevNode);
                });
    }

    @Nonnull
    public Optional<Node> developNodeForwards(@Nonnull Node node) {
        return node
                .getStageDefinition()
                .getNextStage()
                .flatMap(name -> getNodeForStageDefinitionName(name)
                        .or(() -> getCachedStageDefinitionForName(name)
                                .map(stageDefinition -> {
                                    var newNode = new Node( stageDefinition, null);
                                    addNode(newNode);
                                    return newNode;
                                })
                        )
                )
                .map(nextNode -> {
                    node.addNextNode(nextNode);
                    nextNode.addPreviousNode(node);
                    return nextNode;
                });
    }

    @Nonnull
    public Optional<Node> getNodeForStageDefinitionName(@Nonnull String name) {
        return this.nodes
                .stream()
                .filter(s -> Objects.equals(name, s.getStageDefinition().getName()))
                .findFirst();
    }

    @Nonnull
    private Node getNode(int nodeIndex) throws IndexOutOfBoundsException {
        return nodes.get(nodeIndex);
    }

    @Nonnull
    private Optional<StageDefinition> getCachedStageDefinitionForName(@Nonnull String name) {
        return Optional.ofNullable(cachedStageDefinition.get(name));
    }

    @Nonnull
    public Optional<ExecutionGroup> getCachedExecutionGroupForId(@Nonnull ExecutionGroupId id) {
        return Optional.ofNullable(cachedExecutionGroup.get(id));
    }
}
