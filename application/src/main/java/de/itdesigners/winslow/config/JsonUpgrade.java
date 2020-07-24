package de.itdesigners.winslow.config;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public class JsonUpgrade {

    @Nullable
    public static <N> N readNullable(
            @Nonnull JsonNode node,
            @Nonnull ObjectCodec codec,
            @Nonnull String name,
            @Nonnull TypeReference<N> ref) throws IOException {
        var val = node.get(name);
        if (val != null) {
            return val.traverse(codec).readValueAs(ref);
        } else {
            return null;
        }
    }

    @Nullable
    public static <N> N readNullable(
            @Nonnull JsonNode node,
            @Nonnull ObjectCodec codec,
            @Nonnull String name,
            @Nonnull Class<N> clazz) throws IOException {
        var val = node.get(name);
        if (val != null) {
            return val.traverse(codec).readValueAs(clazz);
        } else {
            return null;
        }
    }
}
