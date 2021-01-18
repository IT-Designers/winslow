package de.itdesigners.winslow;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CachedFunction<T, R> implements Function<T, R> {

    private final @Nonnull Function<T, R> function;
    private final @Nonnull Map<T, R>      cache = new HashMap<>();

    public CachedFunction(@Nonnull Function<T, R> function) {
        this.function = function;
    }

    @Override
    public R apply(T parameter) {
        if (this.cache.containsKey(parameter)) {
            return this.cache.get(parameter);
        } else {
            var result = this.function.apply(parameter);
            this.cache.put(parameter, result);
            return result;
        }
    }
}
