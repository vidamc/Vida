/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia;

import dev.vida.core.ApiStatus;
import java.util.Arrays;
import java.util.Objects;

/**
 * Имя одного метода в нескольких namespace.
 *
 * <p>Идентифицируется тройкой {@code (sourceName, sourceDescriptor, owner)} — owner
 * задан контекстом {@link ClassMapping}. Дескриптор — в JVM-форме, хранится в
 * source namespace.
 *
 * <p>Класс иммутабелен.
 */
@ApiStatus.Stable
public final class MethodMapping {

    private final Namespace[] namespaces;
    private final String[] names;
    private final String sourceDescriptor;

    MethodMapping(Namespace[] namespaces, String[] names, String sourceDescriptor) {
        if (namespaces.length != names.length) {
            throw new IllegalArgumentException(
                    "namespaces.length=" + namespaces.length + " != names.length=" + names.length);
        }
        this.namespaces = namespaces;
        this.names = names;
        this.sourceDescriptor = Objects.requireNonNull(sourceDescriptor, "sourceDescriptor");
    }

    public Namespace[] namespaces() {
        return namespaces.clone();
    }

    public String sourceName() {
        return names[0];
    }

    public String sourceDescriptor() {
        return sourceDescriptor;
    }

    public String name(Namespace ns) {
        Objects.requireNonNull(ns, "ns");
        for (int i = 0; i < namespaces.length; i++) {
            if (namespaces[i].equals(ns)) {
                return names[i];
            }
        }
        return null;
    }

    public String name(int namespaceIndex) {
        return names[namespaceIndex];
    }

    @Override
    public String toString() {
        return "MethodMapping{names=" + Arrays.toString(names)
                + ", desc=" + sourceDescriptor + "}";
    }
}
