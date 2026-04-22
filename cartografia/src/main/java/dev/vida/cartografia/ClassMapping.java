/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia;

import dev.vida.core.ApiStatus;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Иммутабельная запись о классе в N пространств имён.
 *
 * <p>Хранит внутренние JVM-имена класса ({@code com/mojang/Foo}) параллельно
 * массиву {@link MappingTree#namespaces()} и списки членов
 * ({@link FieldMapping}, {@link MethodMapping}).
 *
 * <p>Поле-уникально по имени внутри класса (JVM-ограничение), метод — по
 * паре {@code (name, descriptor)}. Для быстрого поиска поддерживаются две
 * индексированные карты.
 */
@ApiStatus.Stable
public final class ClassMapping {

    private final Namespace[] namespaces;
    private final String[] names;

    private final List<FieldMapping> fields;
    private final List<MethodMapping> methods;

    /** Поля, индексированные по {@code sourceName}. Sapporo fields-unique-by-name в JVM. */
    private final Map<String, FieldMapping> fieldByName;

    /** Методы, индексированные по {@code (sourceName, sourceDescriptor)}. */
    private final Map<MemberKey, MethodMapping> methodByKey;

    ClassMapping(
            Namespace[] namespaces,
            String[] names,
            List<FieldMapping> fields,
            List<MethodMapping> methods) {
        if (namespaces.length != names.length) {
            throw new IllegalArgumentException(
                    "namespaces.length=" + namespaces.length + " != names.length=" + names.length);
        }
        this.namespaces = namespaces;
        this.names = names;
        this.fields = List.copyOf(fields);
        this.methods = List.copyOf(methods);

        Map<String, FieldMapping> fByName = new LinkedHashMap<>(this.fields.size() * 2);
        for (FieldMapping f : this.fields) {
            fByName.put(f.sourceName(), f);
        }
        this.fieldByName = Collections.unmodifiableMap(fByName);

        Map<MemberKey, MethodMapping> mByKey = new LinkedHashMap<>(this.methods.size() * 2);
        for (MethodMapping m : this.methods) {
            mByKey.put(new MemberKey(m.sourceName(), m.sourceDescriptor()), m);
        }
        this.methodByKey = Collections.unmodifiableMap(mByKey);
    }

    public Namespace[] namespaces() {
        return namespaces.clone();
    }

    /** Внутреннее имя в source namespace (например, {@code com/mojang/Foo}). */
    public String sourceName() {
        return names[0];
    }

    /** Внутреннее имя в заданном namespace или {@code null}, если ns чужой. */
    public String name(Namespace ns) {
        Objects.requireNonNull(ns, "ns");
        for (int i = 0; i < namespaces.length; i++) {
            if (namespaces[i].equals(ns)) {
                return names[i];
            }
        }
        return null;
    }

    /** Имя по индексу namespace. */
    public String name(int namespaceIndex) {
        return names[namespaceIndex];
    }

    public Collection<FieldMapping> fields() {
        return fields;
    }

    public Collection<MethodMapping> methods() {
        return methods;
    }

    /** Поиск поля по имени в source namespace. */
    public FieldMapping findField(String sourceName) {
        return fieldByName.get(Objects.requireNonNull(sourceName, "sourceName"));
    }

    /** Поиск метода по имени и дескриптору в source namespace. */
    public MethodMapping findMethod(String sourceName, String sourceDescriptor) {
        Objects.requireNonNull(sourceName, "sourceName");
        Objects.requireNonNull(sourceDescriptor, "sourceDescriptor");
        return methodByKey.get(new MemberKey(sourceName, sourceDescriptor));
    }

    @Override
    public String toString() {
        return "ClassMapping{names=" + Arrays.toString(names)
                + ", fields=" + fields.size()
                + ", methods=" + methods.size() + "}";
    }

    /** Ключ (name, descriptor) — внутренний, используется для индексации членов. */
    record MemberKey(String name, String descriptor) {
        MemberKey {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(descriptor, "descriptor");
        }
    }
}
