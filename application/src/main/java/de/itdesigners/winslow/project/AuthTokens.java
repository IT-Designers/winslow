package de.itdesigners.winslow.project;

import org.javatuples.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.ConstructorProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class AuthTokens {

    private final @Nonnull List<AuthToken> tokens;

    @ConstructorProperties({"tokens"})
    public AuthTokens(@Nullable List<AuthToken> tokens) {
        this.tokens = tokens != null ? new ArrayList<>(tokens) : new ArrayList<>();
    }


    public Optional<AuthToken> getTokenForSecret(@Nonnull String secret) {
        return this.tokens
                .stream()
                .filter(t -> t.isSecret(secret))
                .findFirst();
    }

    public Optional<AuthToken> getTokenForId(@Nonnull String id) {
        return this.tokens
                .stream()
                .filter(t -> t.getId().equals(id))
                .findFirst();
    }

    @Nonnull
    public Stream<AuthToken> getTokens() {
        return this.tokens.stream();
    }

    @Nonnull
    public Pair<AuthToken, String> createToken(@Nonnull String name) {
        var id     = createTokenId();
        var secret = createTokenSecret();
        var token  = new AuthToken(id, secret, name, null);
        this.tokens.add(token);
        return new Pair<>(token, secret);
    }

    @Nonnull
    private String createTokenId() {
        String id;
        do {
            id = UUID.randomUUID().toString();
        } while (getTokenForId(id).isPresent());
        return id;
    }

    @Nonnull
    private String createTokenSecret() {
        return UUID.randomUUID().toString();
    }

    public Optional<AuthToken> deleteTokenForId(@Nonnull String id) {
        var token = getTokenForId(id);
        token.ifPresent(this.tokens::remove);
        return token;
    }
}
