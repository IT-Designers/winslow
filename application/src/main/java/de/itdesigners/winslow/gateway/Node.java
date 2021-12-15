package de.itdesigners.winslow.gateway;

import de.itdesigners.winslow.config.ExecutionGroup;
import de.itdesigners.winslow.config.StageDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Node {

    private final @Nonnull List<Node>           previousNodes;
    private final @Nonnull List<Node>           nextNodes;
    private final @Nonnull List<ExecutionGroup> executionGroups;
    private final @Nonnull StageDefinition      stageDefinition;

    public Node(
            @Nonnull StageDefinition stageDefinition,
            @Nullable ExecutionGroup executionGroup) {
        this(stageDefinition, executionGroup, null, null);
    }

    public Node(
            @Nonnull StageDefinition stageDefinition,
            @Nullable ExecutionGroup executionGroup,
            @Nullable Node previousNodeIndex,
            @Nullable Node nextNodeIndex) {
        this.stageDefinition = stageDefinition;
        this.executionGroups = Optional.ofNullable(executionGroup).stream().collect(Collectors.toList());
        this.previousNodes   = Optional.ofNullable(previousNodeIndex).stream().collect(Collectors.toList());
        this.nextNodes       = Optional.ofNullable(nextNodeIndex).stream().collect(Collectors.toList());
    }

    @Nonnull
    public List<Node> getPreviousNodes() {
        return this.previousNodes;
    }

    @Nonnull
    public List<Node> getNextNodes() {
        return this.nextNodes;
    }

    @Nonnull
    public List<ExecutionGroup> getExecutionGroups() {
        return this.executionGroups;
    }

    @Nonnull
    public StageDefinition getStageDefinition() {
        return this.stageDefinition;
    }

    @Nonnull
    public Optional<ExecutionGroup> getFirstExecutionGroup() {
        if (this.executionGroups.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(this.executionGroups.get(0));
        }
    }

    @Nonnull
    public Node getNode() {
        return this;
    }

    /**
     * @param node to add to the previous nodes this node
     */
    public void addPreviousNode(@Nonnull Node node) {
        if (!previousNodes.contains(node)) {
            previousNodes.add(node);
        }
    }

    /**
     * @param node to add to the next nodes this node
     */
    public void addNextNode(@Nonnull Node node) {
        if (!nextNodes.contains(node)) {
            nextNodes.add(node);
        }
    }

    /**
     * @param executionGroup to add to the execution groups of this node
     */
    public void addExecutionGroup(@Nonnull ExecutionGroup executionGroup) {
        executionGroups.add(executionGroup);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Node node = (Node) o;
        return stageDefinition.equals(node.stageDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stageDefinition);
    }

    @Nonnull
    public String toString() {
        return "Node@{name='" + getStageDefinition().getName() + "'}#" + hashCode();
    }
}
