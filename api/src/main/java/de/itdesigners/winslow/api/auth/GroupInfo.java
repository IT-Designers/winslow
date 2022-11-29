package de.itdesigners.winslow.api.auth;

import javax.annotation.Nonnull;
import java.util.List;

public record GroupInfo(
        @Nonnull String name,
        @Nonnull List<Link> members) {
}
