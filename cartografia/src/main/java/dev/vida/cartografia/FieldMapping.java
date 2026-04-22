/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia;

import dev.vida.core.ApiStatus;
import java.util.Arrays;
import java.util.Objects;

/**
 * Имя одного поля в нескольких namespace.
 *
 * <p>Поле идентифицируется парой {@code (sourceName, sourceDescriptor)} в
 * источниковом namespace родительского {@link MappingTree}. Имена в других
 * namespace хранятся параллельно массиву {@link #namespaces()}.
 *
 * <p>Дескриптор — в JVM-форме (например, {@code I}, {@code Ljava/lang/String;},
 * {@code [[Lnet/minecraft/foo/Bar;}), записан в source namespace. Для получения
 * дескриптора в другом namespace используйте
 * {@link MappingTree#remapDescriptor(Namespace, Namespace, String)}.
 *
 * <p>Класс иммутабелен; массивы имён не экспортируются наружу.
 */
@ApiStatus.Stable
public final class FieldMapping {

    private final Namespace[] namespaces; // shared immutable reference
    private final String[] names;         // parallel to namespaces
    private final String sourceDescriptor;

    FieldMapping(Namespace[] namespaces, String[] names, String sourceDescriptor) {
        if (namespaces.length != names.length) {
            throw new IllegalArgumentException(
                    "namespaces.length=" + namespaces.length + " != names.length=" + names.length);
        }
        this.namespaces = namespaces;
        this.names = names;
        this.sourceDescriptor = Objects.requireNonNull(sourceDescriptor, "sourceDescriptor");
    }

    /** Namespace-массив родительского дерева. */
    public Namespace[] namespaces() {
        return namespaces.clone();
    }

    /** Имя в source namespace (индекс 0). */
    public String sourceName() {
        return names[0];
    }

    /** Дескриптор поля в source namespace. */
    public String sourceDescriptor() {
        return sourceDescriptor;
    }

    /** Имя в заданном namespace или {@code null}, если namespace не принадлежит дереву. */
    public String name(Namespace ns) {
        Objects.requireNonNull(ns, "ns");
        for (int i = 0; i < namespaces.length; i++) {
            if (namespaces[i].equals(ns)) {
                return names[i];
            }
        }
        return null;
    }

    /** Имя по индексу namespace (совпадает с порядком в {@link MappingTree#namespaces()}). */
    public String name(int namespaceIndex) {
        return names[namespaceIndex];
    }

    @Override
    public String toString() {
        return "FieldMapping{names=" + Arrays.toString(names)
                + ", desc=" + sourceDescriptor + "}";
    }
}
