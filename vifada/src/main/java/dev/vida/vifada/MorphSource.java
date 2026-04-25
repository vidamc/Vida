/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Источник байткода морфа — имя класса и его {@code .class}-байты.
 * Обычно создаётся дискавери-слоем; для тестов — руками.
 */
@ApiStatus.Stable
public record MorphSource(String internalName, byte[] bytes) {

    public MorphSource {
        Objects.requireNonNull(internalName, "internalName");
        Objects.requireNonNull(bytes, "bytes");
        if (internalName.isBlank()) {
            throw new IllegalArgumentException("internalName must not be blank");
        }
        if (bytes.length == 0) {
            throw new IllegalArgumentException("bytes must not be empty");
        }
    }

    /** Хелпер: конвертация FQN {@code a.b.Foo} в internal {@code a/b/Foo}. */
    public static String fqnToInternal(String fqn) {
        Objects.requireNonNull(fqn, "fqn");
        return fqn.replace('.', '/');
    }

    /** Хелпер: конвертация internal {@code a/b/Foo} в FQN {@code a.b.Foo}. */
    public static String internalToFqn(String internal) {
        Objects.requireNonNull(internal, "internal");
        return internal.replace('/', '.');
    }
}
