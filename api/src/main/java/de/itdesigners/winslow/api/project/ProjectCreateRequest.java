package de.itdesigners.winslow.api.project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.Transient;
import java.util.List;
import java.util.Optional;

public record ProjectCreateRequest(
        @Nonnull String name,
        @Nonnull String pipeline,
        @Nullable List<String> tags) {

    @Nonnull
    @Transient
    public Optional<List<String>> optTags() {
        return Optional.ofNullable(tags);
    }

}
