package de.itdesigners.winslow.api.project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ProjectCreateRequest {
    public @Nonnull  String       name;
    public @Nonnull  String       pipeline;
    public @Nullable List<String> tags;

    public ProjectCreateRequest(
            @Nonnull String name,
            @Nonnull String pipeline,
            @Nullable List<String> tags) {
        this.name     = name;
        this.pipeline = pipeline;
        this.tags     = tags;
    }
}
