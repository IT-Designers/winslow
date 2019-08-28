package de.itd.tracking.winslow.auth;

import java.util.ArrayList;
import java.util.List;

public class Group {

    private final String       name;
    private final List<String> users = new ArrayList<>();

    public Group(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isMember(String name) {
        return this.users.contains(name);
    }

    public Group withUser(String name) {
        if (!this.users.contains(name)) {
            this.users.add(name);
        }
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{name='" + this.name + "'}#" + this.hashCode();
    }
}
