package de.itd.tracking.winslow.auth;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

public class User {

    private final String                  name;
    private final GroupAssignmentResolver resolver;

    public User(String name, GroupAssignmentResolver resolver) {
        this.name     = name;
        this.resolver = resolver;
    }

    public String getName() {
        return this.name;
    }

    public boolean canAccessGroup(String group) {
        return this.resolver.canAccessGroup(this.name, group);
    }

    @Nonnull
    public Stream<String> getGroups() {
        return this.resolver.getAssignedGroups(this.name);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@{name='" + this.name + "'}#" + hashCode();
    }
}
