package de.itdesigners.winslow.web;

import de.itdesigners.winslow.api.project.AuthTokenInfo;
import de.itdesigners.winslow.project.AuthToken;

import javax.annotation.Nonnull;
import java.util.stream.Collectors;

public class AuthTokenInfoConverter {

    @Nonnull
    public static AuthTokenInfo convert(@Nonnull AuthToken token) {
        return new AuthTokenInfo(
                token.getId(),
                null,
                token.getName(),
                token.getCapabilities().collect(Collectors.toList())
        );
    }

}
