package de.itdesigners.winslow.api.auth;

import javax.annotation.Nonnull;

public record Link(@Nonnull String name, @Nonnull Role role) {}
