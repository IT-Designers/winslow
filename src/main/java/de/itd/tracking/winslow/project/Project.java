package de.itd.tracking.winslow.project;

import de.itd.tracking.winslow.Orchestrator;
import de.itd.tracking.winslow.Pipeline;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class Project {

    private final Orchestrator orchestrator;
    private final String       id;
    private final String       owner;
    private final List<String> groups = new ArrayList<>();

    private String name;

    public Project(Orchestrator orchestrator, String id, String owner) {
        this(orchestrator, id, owner, "");
    }

    public Project(Orchestrator orchestrator, String id, String owner, String name) {
        this.orchestrator = orchestrator;
        this.id           = id;
        this.owner        = owner;
        this.name         = name;
    }


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public Iterable<String> getGroups() {
        return Collections.unmodifiableList(this.groups);
    }

    public void addGroup(String group) {
        if (!this.groups.contains(group)) {
            this.groups.add(group);
        }
    }

    public boolean removeGroup(String group) {
        return this.groups.remove(group);
    }

    @Nonnull
    public Optional<Pipeline> getPipeline() {
        return this.orchestrator.getPipeline(this);
    }

    @Nonnull
    public <T> Optional<T> updatePipeline(@Nonnull Function<Pipeline, T> updater) {
        return this.orchestrator.updatePipeline(this, updater);
    }
}
