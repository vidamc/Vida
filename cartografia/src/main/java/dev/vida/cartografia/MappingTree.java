/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia;

import dev.vida.core.ApiStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Иммутабельное дерево мэппингов — основной тип Cartografía.
 *
 * <p>Дерево описывает N ≥ 1 упорядоченных {@link Namespace}; первый элемент
 * называется <em>source namespace</em> и задаёт канонический ключ: все
 * внутренние имена классов и дескрипторы членов хранятся именно в нём.
 *
 * <h2>Типичная схема использования</h2>
 * <ol>
 *   <li>Построить дерево через {@link #builder(Namespace, Namespace...)} или
 *       {@linkplain dev.vida.cartografia.io.ProguardReader
 *       io.ProguardReader}/{@linkplain dev.vida.cartografia.io.CtgReader
 *       io.CtgReader}.</li>
 *   <li>Искать имена: {@link #classByName(Namespace, String)},
 *       {@link #classBySource(String)}.</li>
 *   <li>Для runtime-ремапинга использовать
 *       {@link dev.vida.cartografia.asm.CartografiaRemapper}.</li>
 * </ol>
 *
 * <p>Все коллекции, возвращаемые наружу, — immutable views. Class/field/method
 * идентичность гарантируется только в рамках одного дерева.
 */
@ApiStatus.Stable
public final class MappingTree {

    private final Namespace[] namespaces;
    private final List<ClassMapping> classes;

    /** Индекс source namespace (0) — всегда строится в конструкторе. */
    private final Map<String, ClassMapping> bySourceName;

    /**
     * Индексы для namespace с индексом ≥ 1 строятся лениво при первом
     * {@link #classByName(Namespace, String)} — типичный lookup идёт по
     * source, вторичные namespace нужны реже.
     */
    private final ConcurrentHashMap<Namespace, Map<String, ClassMapping>> lazyByNamespace;

    private MappingTree(Namespace[] namespaces, List<ClassMapping> classes) {
        this.namespaces = namespaces;
        this.classes = List.copyOf(classes);
        this.lazyByNamespace = new ConcurrentHashMap<>(Math.max(0, namespaces.length - 1));

        Map<String, ClassMapping> src = new HashMap<>(this.classes.size() * 2);
        for (ClassMapping c : this.classes) {
            String name = c.name(0);
            if (name != null && !name.isEmpty()) {
                src.put(name, c);
            }
        }
        this.bySourceName = Collections.unmodifiableMap(src);
    }

    private Map<String, ClassMapping> indexFor(Namespace ns) {
        if (ns.equals(namespaces[0])) {
            return bySourceName;
        }
        return lazyByNamespace.computeIfAbsent(ns, this::buildIndexForNamespace);
    }

    private Map<String, ClassMapping> buildIndexForNamespace(Namespace ns) {
        int idx = indexOf(ns);
        if (idx < 0) {
            return Map.of();
        }
        Map<String, ClassMapping> m = new HashMap<>(classes.size() * 2);
        for (ClassMapping c : classes) {
            String name = c.name(idx);
            if (name != null && !name.isEmpty()) {
                m.put(name, c);
            }
        }
        return Collections.unmodifiableMap(m);
    }

    // =============================================================== factory

    /** Builder-конструктор для программного создания дерева. */
    public static Builder builder(Namespace source, Namespace... targets) {
        Objects.requireNonNull(source, "source");
        Namespace[] arr = new Namespace[1 + targets.length];
        arr[0] = source;
        System.arraycopy(targets, 0, arr, 1, targets.length);
        return new Builder(arr);
    }

    // =============================================================== query

    /** Упорядоченный список namespace; index 0 — source. */
    public List<Namespace> namespaces() {
        return List.of(namespaces);
    }

    /** Source namespace — index 0. */
    public Namespace source() {
        return namespaces[0];
    }

    /** Индекс namespace в массиве или {@code -1}. */
    public int indexOf(Namespace ns) {
        for (int i = 0; i < namespaces.length; i++) {
            if (namespaces[i].equals(ns)) {
                return i;
            }
        }
        return -1;
    }

    public Collection<ClassMapping> classes() {
        return classes;
    }

    /** Общее количество классов. */
    public int size() {
        return classes.size();
    }

    /** Класс по его имени в source namespace. */
    public ClassMapping classBySource(String internalName) {
        return bySourceName.get(Objects.requireNonNull(internalName, "internalName"));
    }

    /**
     * Класс по его имени в заданном namespace.
     *
     * @return соответствующий {@link ClassMapping} или {@code null}, если
     *         имя неизвестно либо namespace отсутствует в дереве.
     */
    public ClassMapping classByName(Namespace ns, String internalName) {
        Objects.requireNonNull(ns, "ns");
        Objects.requireNonNull(internalName, "internalName");
        Map<String, ClassMapping> m = indexFor(ns);
        return m.get(internalName);
    }

    // =========================================================== remapping

    /**
     * Переписывает JVM-дескриптор из namespace {@code from} в {@code to}.
     *
     * <p>Внутренние имена классов ({@code Lcom/foo/Bar;}, {@code [[Lfoo/Baz;})
     * заменяются целевыми; примитивы остаются как есть. Если имя класса
     * неизвестно дереву, оно не переписывается.
     *
     * @throws IllegalArgumentException если {@code from} или {@code to} не из
     *         этого дерева.
     */
    public String remapDescriptor(Namespace from, Namespace to, String descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        if (from.equals(to)) {
            return descriptor;
        }
        int fromIdx = indexOf(from);
        int toIdx = indexOf(to);
        if (fromIdx < 0) throw new IllegalArgumentException("unknown namespace: " + from);
        if (toIdx < 0) throw new IllegalArgumentException("unknown namespace: " + to);

        StringBuilder out = new StringBuilder(descriptor.length() + 8);
        int i = 0, n = descriptor.length();
        while (i < n) {
            char c = descriptor.charAt(i);
            if (c == 'L') {
                int end = descriptor.indexOf(';', i + 1);
                if (end < 0) {
                    out.append(descriptor, i, n);
                    break;
                }
                String cls = descriptor.substring(i + 1, end);
                ClassMapping cm = indexFor(from).get(cls);
                String replaced = (cm != null) ? cm.name(toIdx) : cls;
                out.append('L').append(replaced == null ? cls : replaced).append(';');
                i = end + 1;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    // =============================================================== builder

    /**
     * Мутабельный помощник построения {@link MappingTree}.
     *
     * <p>Не потокобезопасен; рассчитан на однопроходную сборку (парсер ->
     * builder -> {@link #build()}).
     */
    public static final class Builder {

        private final Namespace[] namespaces;
        private final List<ClassMapping> classes = new ArrayList<>(1024);
        private final Map<String, Integer> classIndexBySource = new HashMap<>(2048);

        Builder(Namespace[] namespaces) {
            if (namespaces.length == 0) {
                throw new IllegalArgumentException("at least one namespace required");
            }
            Namespace[] copy = namespaces.clone();
            // Проверка на дубликаты namespace — они должны быть уникальны.
            for (int i = 0; i < copy.length; i++) {
                for (int j = i + 1; j < copy.length; j++) {
                    if (copy[i].equals(copy[j])) {
                        throw new IllegalArgumentException("duplicate namespace: " + copy[i]);
                    }
                }
            }
            this.namespaces = copy;
        }

        public Namespace[] namespaces() {
            return namespaces.clone();
        }

        /** Количество namespace. */
        public int namespaceCount() {
            return namespaces.length;
        }

        /**
         * Добавляет новый класс. Количество имён должно равняться
         * {@link #namespaceCount()}; индекс 0 — source namespace.
         */
        public ClassBuilder addClass(String... namesInNsOrder) {
            if (namesInNsOrder.length != namespaces.length) {
                throw new IllegalArgumentException(
                        "expected " + namespaces.length + " names, got " + namesInNsOrder.length);
            }
            String source = namesInNsOrder[0];
            Objects.requireNonNull(source, "source name");
            if (source.isEmpty()) {
                throw new IllegalArgumentException("source class name is empty");
            }
            if (classIndexBySource.containsKey(source)) {
                throw new IllegalStateException("duplicate class in source namespace: " + source);
            }
            ClassBuilder cb = new ClassBuilder(namespaces, namesInNsOrder.clone());
            classIndexBySource.put(source, classes.size());
            classes.add(null); // placeholder — заменим в build()
            cb.builderIndex = classes.size() - 1;
            cb.parent = this;
            return cb;
        }

        /** Строит иммутабельное дерево и закрывает builder. */
        public MappingTree build() {
            for (int i = 0; i < classes.size(); i++) {
                if (classes.get(i) == null) {
                    throw new IllegalStateException("class at index " + i + " was never finished");
                }
            }
            return new MappingTree(namespaces, classes);
        }

        void replaceClass(int index, ClassMapping cm) {
            classes.set(index, cm);
        }
    }

    /** Строитель отдельного класса. */
    public static final class ClassBuilder {

        private final Namespace[] namespaces;
        private final String[] names;
        private final List<FieldMapping> fields = new ArrayList<>(8);
        private final List<MethodMapping> methods = new ArrayList<>(8);

        Builder parent;
        int builderIndex;

        ClassBuilder(Namespace[] namespaces, String[] names) {
            this.namespaces = namespaces;
            this.names = names;
        }

        /**
         * Добавляет поле.
         *
         * @param sourceDescriptor JVM-дескриптор поля в source namespace
         * @param namesInNsOrder имена поля параллельно namespace-массиву дерева
         */
        public ClassBuilder addField(String sourceDescriptor, String... namesInNsOrder) {
            checkArity(namesInNsOrder);
            fields.add(new FieldMapping(namespaces, namesInNsOrder.clone(), sourceDescriptor));
            return this;
        }

        /**
         * Добавляет метод.
         *
         * @param sourceDescriptor JVM-дескриптор метода в source namespace
         * @param namesInNsOrder имена метода параллельно namespace-массиву дерева
         */
        public ClassBuilder addMethod(String sourceDescriptor, String... namesInNsOrder) {
            checkArity(namesInNsOrder);
            methods.add(new MethodMapping(namespaces, namesInNsOrder.clone(), sourceDescriptor));
            return this;
        }

        /** Возвращается к родителю, завершая описание класса. */
        public Builder done() {
            if (parent == null) {
                throw new IllegalStateException("ClassBuilder already closed");
            }
            parent.replaceClass(builderIndex, new ClassMapping(namespaces, names, fields, methods));
            Builder p = parent;
            parent = null;
            return p;
        }

        private void checkArity(String[] arr) {
            if (arr.length != namespaces.length) {
                throw new IllegalArgumentException(
                        "expected " + namespaces.length + " names, got " + arr.length);
            }
        }
    }

    @Override
    public String toString() {
        return "MappingTree{namespaces=" + Arrays.toString(namespaces)
                + ", classes=" + classes.size() + "}";
    }
}
