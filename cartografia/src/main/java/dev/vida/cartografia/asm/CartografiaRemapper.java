/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia.asm;

import dev.vida.cartografia.ClassMapping;
import dev.vida.cartografia.FieldMapping;
import dev.vida.cartografia.MappingTree;
import dev.vida.cartografia.MethodMapping;
import dev.vida.cartografia.Namespace;
import dev.vida.core.ApiStatus;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.objectweb.asm.commons.Remapper;

/**
 * Адаптер {@link Remapper} над {@link MappingTree}.
 *
 * <p>Применяется к байткоду через {@code ClassRemapper}, {@code MethodRemapper}
 * и другие классы из {@code org.objectweb.asm.commons}. Направление перевода —
 * из {@linkplain MappingTree#source() source namespace} дерева в указанный
 * целевой namespace.
 *
 * <p>Для обратного перевода (например, {@code named → obf}) постройте
 * инвертированное {@link MappingTree} и создайте новый ремаппер.
 *
 * <p>Тип потокобезопасен для чтения: кэш имён классов защищён
 * {@link ConcurrentHashMap}.
 */
@ApiStatus.Stable
public final class CartografiaRemapper extends Remapper {

    /** Собственный маркер «не найдено» — поскольку ConcurrentHashMap не принимает {@code null}. */
    private static final String NOT_FOUND = new String("\u0000NF");

    private final MappingTree tree;
    private final Namespace to;
    private final int toIndex;
    private final ConcurrentMap<String, String> classCache = new ConcurrentHashMap<>();

    private CartografiaRemapper(MappingTree tree, Namespace to, int toIndex) {
        this.tree = tree;
        this.to = to;
        this.toIndex = toIndex;
    }

    /**
     * Фабрика: source → target.
     *
     * @throws IllegalArgumentException если {@code target} не принадлежит дереву.
     */
    public static CartografiaRemapper of(MappingTree tree, Namespace target) {
        Objects.requireNonNull(tree, "tree");
        Objects.requireNonNull(target, "target");
        int idx = tree.indexOf(target);
        if (idx < 0) {
            throw new IllegalArgumentException(
                    "namespace '" + target + "' is not part of this mapping tree");
        }
        return new CartografiaRemapper(tree, target, idx);
    }

    /** Дерево, над которым работает этот ремаппер. */
    public MappingTree tree() {
        return tree;
    }

    /** Целевой namespace. */
    public Namespace target() {
        return to;
    }

    // =============================================================== Remapper

    @Override
    public String map(String internalName) {
        if (internalName == null) {
            return null;
        }
        String cached = classCache.get(internalName);
        if (cached == null) {
            cached = computeClass(internalName);
            classCache.put(internalName, cached);
        }
        return cached == NOT_FOUND ? internalName : cached;
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        ClassMapping cm = tree.classBySource(owner);
        if (cm == null) return name;
        FieldMapping fm = cm.findField(name);
        if (fm == null) return name;
        String mapped = fm.name(toIndex);
        return (mapped == null || mapped.isEmpty()) ? name : mapped;
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        ClassMapping cm = tree.classBySource(owner);
        if (cm == null) return name;
        MethodMapping mm = cm.findMethod(name, descriptor);
        if (mm == null) return name;
        String mapped = mm.name(toIndex);
        return (mapped == null || mapped.isEmpty()) ? name : mapped;
    }

    @Override
    public String mapRecordComponentName(String owner, String name, String descriptor) {
        return mapFieldName(owner, name, descriptor);
    }

    /**
     * Для {@code invokedynamic}-целей owner неизвестен; без дополнительного
     * анализа lambda-фабрики мы не можем безопасно переименовать. Сохраняем
     * имя без изменений.
     */
    @Override
    public String mapInvokeDynamicMethodName(String name, String descriptor) {
        return name;
    }

    // =============================================================== helpers

    private String computeClass(String internalName) {
        ClassMapping cm = tree.classBySource(internalName);
        if (cm == null) return NOT_FOUND;
        String mapped = cm.name(toIndex);
        return (mapped == null || mapped.isEmpty()) ? NOT_FOUND : mapped;
    }
}
