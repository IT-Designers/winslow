package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.config.PipelineDefinition;
import de.itdesigners.winslow.config.StageDefinition;
import de.itdesigners.winslow.config.StageWorkerDefinition;
import de.itdesigners.winslow.pipeline.ExecutionGroupId;
import de.itdesigners.winslow.pipeline.Pipeline;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class Graph {

    private final @Nonnull List<Node>                            nodes;
    private final @Nonnull Map<ExecutionGroupId, ExecutionGroup> cachedExecutionGroup;
    private final @Nonnull Map<UUID, StageDefinition>            cachedStageDefinition;

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

        pipelineDefinition.stages().forEach(s -> cachedStageDefinition.put(s.id(), s));

        addNode(rootNode);
        findAllDirectlyConnectedNodes();
        developNodeBackwardsByEG(rootNode);
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
            if (node.getStageDefinition().nextStages().contains(def.id())) {
                getOrCreateNodeForStageDefinitionId(def.id()).ifPresent(nextNode -> {
                    node.addNextNode(nextNode);
                    nextNode.addPreviousNode(node);
                });
            }
            if (def.nextStages().contains(node.getStageDefinition().id())) {
                getOrCreateNodeForStageDefinitionId(def.id()).ifPresent(prevNode -> {
                    node.addPreviousNode(prevNode);
                    prevNode.addNextNode(node);
                });
            }
        }
    }

    public void developNodeBackwardsByEG(@Nonnull Node node) {
        node
                .getExecutionGroups()
                .stream()
                .flatMap(group -> group.getParentId().stream())
                .flatMap(parentId -> getCachedExecutionGroupForGroupId(parentId).stream())
                .flatMap(exg -> getOrCreateNodeForStageDefinitionId(exg.getStageDefinition().id())
                        .stream()
                        .peek(n -> n.addExecutionGroup(exg))
                )
                .peek(prevNode -> {
                    node.addPreviousNode(prevNode);
                    prevNode.addNextNode(node);
                })
                .collect(Collectors.toList())
                .forEach(prevNode -> {
                    developNodeBackwardsByEG(prevNode);
                    developNodeForwardsByEG(prevNode);
                });
    }

    public void developNodeForwardsByEG(@Nonnull Node node) {
        node
                .getExecutionGroups()
                .stream()
                .flatMap(group -> {
                    var groupId = group.getId();
                    var children = new ArrayList<ExecutionGroup>();
                    for (var eg : cachedExecutionGroup.values()) {
                        if (eg.getParentId().map(pid -> pid.equals(groupId)).orElse(false)) {
                            children.add(eg);
                        }
                    }
                    return children.stream();
                })
                .flatMap(exg -> getOrCreateNodeForStageDefinitionId(exg.getStageDefinition().id())
                        .stream()
                        .peek(n -> n.addExecutionGroup(exg))
                )
                .peek(nextNode -> {
                    node.addNextNode(nextNode);
                    nextNode.addPreviousNode(node);
                })
                .collect(Collectors.toList())
                .forEach(this::developNodeForwardsByEG);
    }

    public void developNodeForwards(@Nonnull Node node) {
        node
                .getStageDefinition()
                .nextStages()
                .stream()
                .flatMap(s -> getOrCreateNodeForStageDefinitionId(s).stream())
                .forEach(nextNode -> {
                    node.addNextNode(nextNode);
                    nextNode.addPreviousNode(node);
                });
    }

    private Optional<Node> getOrCreateNodeForStageDefinitionId(UUID id) {
        return getNodeForStageDefinitionId(id)
                .or(() -> getCachedStageDefinitionForId(id)
                        .map(stageDefinition -> {
                            var newNode = new Node(stageDefinition, null);
                            addNode(newNode);
                            return newNode;
                        })
                );
    }

    @Nonnull
    public Optional<Node> getNodeForStageDefinitionId(@Nonnull UUID id) {
        return this.nodes
                .stream()
                .filter(s -> Objects.equals(id, s.getStageDefinition().id()))
                .findFirst();
    }

    @Nonnull
    private Optional<StageDefinition> getCachedStageDefinitionForId(@Nonnull UUID id) {
        return Optional.ofNullable(cachedStageDefinition.get(id));
    }

    @Nonnull
    public Optional<ExecutionGroup> getCachedExecutionGroupForGroupId(@Nonnull ExecutionGroupId id) {
        return Optional.ofNullable(cachedExecutionGroup.get(id));
    }
}
