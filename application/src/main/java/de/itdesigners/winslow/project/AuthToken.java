package de.itdesigners.winslow.project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

public class AuthToken {

    private final @Nonnull String id;
    private final @Nonnull String secret;
    private final @Nonnull String      name;
    private final @Nonnull Set<String> capabilities;

    public AuthToken(
            @Nonnull String id,
            @Nonnull String secret,
            @Nonnull String name,
            @Nullable Set<String> capabilities) {
        this.id           = id;
        this.secret       = secret;
        this.name         = name;
        this.capabilities = capabilities != null ? new HashSet<>(capabilities) : new HashSet<>();
    }


    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    @Deprecated // used only by the serializer
    public String getSecret() {
        return this.secret;
    }

    public boolean isSecret(@Nonnull String probe) {
        return probe.equals(this.secret);
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public boolean hasCapability(@Nonnull String capability) {
        return this.capabilities.contains(capability);
    }

    @Nonnull
    public Stream<String> getCapabilities() {
        return this.capabilities.stream();
    }

    public AuthToken addCapability(@Nonnull String capability) {
        this.capabilities.add(capability);
        return this;
    }

    public AuthToken removeCapability(@Nonnull String capability) {
        this.capabilities.remove(capability);
        return this;
    }
}
