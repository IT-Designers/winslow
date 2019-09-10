package de.itd.tracking.winslow.project;

import de.itd.tracking.winslow.Orchestrator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Project {

    private final String       id;
    private final String       owner;
    private final List<String> groups = new ArrayList<>();

    private String name;

    public Project(String id, String owner) {
        this(id, owner, "");
    }

    public Project(String id, String owner, String name) {
        this.id    = id;
        this.owner = owner;
        this.name  = name;
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
}
