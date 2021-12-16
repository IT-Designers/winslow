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

    public Graph(
            @Nonnull Pipeline pipelineReadOnly,
            @Nonnull PipelineDefinition pipelineDefinition,
            @Nonnull Node rootNode) {
        this.nodes                 = new ArrayList<>();
        this.cachedExecutionGroup  = new HashMap<>();
        this.cachedStageDefinition = new HashMap<>();

        pipelineReadOnly.getActiveAndPastExecutionGroups().forEach(executionGroup -> {
            cachedExecutionGroup.put(executionGroup.getId(), executionGroup);
        });

        pipelineDefinition.getStages().forEach(s -> cachedStageDefinition.put(s.getName(), s));

        addNode(rootNode);
        findAllDirectlyConnectedNodes();
        developNodeBackwards(rootNode);
        developNodeForwardsForAllNodes(pipelineDefinition);
        findAllDirectlyConnectedNodes();
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

    public void findAllDirectlyConnectedNodes() {
        for (var i = 0; i < nodes.size(); i++) {
            // as the node is linking connected nodes, new nodes are added to the nodes-list
            // which means nodes.size() gets bigger, thus this for-loop will iterate over them...?
            findDirectlyConnectedNodes(nodes.get(i));
        }
    }

    public void findDirectlyConnectedNodes(@Nonnull Node node) {
        for (var def : this.cachedStageDefinition.values()) {
            if (node.getStageDefinition().getNextStages().contains(def.getName())) {
                getOrCreateNodeForStageDefinitionName(def.getName()).ifPresent(nextNode -> {
                    node.addNextNode(nextNode);
                    nextNode.addPreviousNode(node);
                });
            }
            if (def.getNextStages().contains(node.getStageDefinition().getName())) {
                getOrCreateNodeForStageDefinitionName(def.getName()).ifPresent(prevNode -> {
                    node.addPreviousNode(prevNode);
                    prevNode.addNextNode(node);
                });
            }
        }
    }

    public void developNodeBackwards(@Nonnull Node node) {
        node
                .getExecutionGroups()
                .stream()
                .flatMap(group -> group.getParentId().stream())
                .flatMap(parentId -> getCachedExecutionGroupForId(parentId).stream())
                .flatMap(exg -> getOrCreateNodeForStageDefinitionName(exg.getStageDefinition().getName())
                        .stream()
                        .peek(n -> n.addExecutionGroup(exg))
                )
                .forEach(prevNode -> {
                    node.addPreviousNode(prevNode);
                    prevNode.addNextNode(node);
                    developNodeBackwards(prevNode);
                });
    }

    public void developNodeForwards(@Nonnull Node node) {
        node
                .getStageDefinition()
                .getNextStages()
                .stream()
                .flatMap(s -> getOrCreateNodeForStageDefinitionName(s).stream())
                .forEach(nextNode -> {
                    node.addNextNode(nextNode);
                    nextNode.addPreviousNode(node);
                });
    }

    private Optional<Node> getOrCreateNodeForStageDefinitionName(String name) {
        return getNodeForStageDefinitionName(name)
                .or(() -> getCachedStageDefinitionForName(name)
                        .map(stageDefinition -> {
                            var newNode = new Node(stageDefinition, null);
                            addNode(newNode);
                            return newNode;
                        })
                );
    }

    @Nonnull
    public Optional<Node> getNodeForStageDefinitionName(@Nonnull String name) {
        return this.nodes
                .stream()
                .filter(s -> Objects.equals(name, s.getStageDefinition().getName()))
                .findFirst();
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
