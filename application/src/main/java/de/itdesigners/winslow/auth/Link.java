package de.itdesigners.winslow.auth;

import javax.annotation.Nonnull;

public record Link(@Nonnull String name, @Nonnull Role role) {}
