package de.itdesigners.winslow.util;


@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Throwable> {
    R apply(T var1) throws E;
}
