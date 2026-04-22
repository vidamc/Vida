/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia.io;

import dev.vida.cartografia.ClassMapping;
import dev.vida.cartografia.FieldMapping;
import dev.vida.cartografia.MappingError;
import dev.vida.cartografia.MappingTree;
import dev.vida.cartografia.MethodMapping;
import dev.vida.cartografia.Namespace;
import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Запись {@link MappingTree} в бинарный формат {@link CtgFormat .ctg}.
 *
 * <p>Сначала выполняется проход по дереву, чтобы собрать строковый пул, затем
 * линейная запись заголовка, пула, namespace и секции классов. Все числа —
 * big-endian, как у {@link DataOutputStream}.
 *
 * <p>Класс не закрывает переданный {@link OutputStream} — это забота вызывающего.
 */
@ApiStatus.Stable
public final class CtgWriter {

    private CtgWriter() {}

    /**
     * Пишет дерево в поток, возвращая {@link Result#ok} при успехе.
     *
     * <p>Ошибки I/O оборачиваются в {@link MappingError.IoError}.
     */
    public static Result<Void, MappingError> write(MappingTree tree, OutputStream rawOut) {
        Objects.requireNonNull(tree, "tree");
        Objects.requireNonNull(rawOut, "out");

        StringPoolBuilder pool = collect(tree);

        try {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(rawOut));
            out.write(CtgFormat.MAGIC);
            out.writeByte(CtgFormat.VERSION_MAJOR);
            out.writeByte(CtgFormat.VERSION_MINOR);
            out.writeShort(0); // flags

            // String pool
            List<String> strings = pool.ordered();
            out.writeInt(strings.size());
            for (String s : strings) {
                byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                out.writeInt(bytes.length);
                out.write(bytes);
            }

            // Namespaces
            List<Namespace> namespaces = tree.namespaces();
            out.writeShort(namespaces.size());
            for (Namespace ns : namespaces) {
                out.writeInt(pool.indexOf(ns.name()));
            }

            // Classes
            out.writeInt(tree.size());
            for (ClassMapping cm : tree.classes()) {
                for (int i = 0; i < namespaces.size(); i++) {
                    out.writeInt(pool.indexOf(nullToEmpty(cm.name(i))));
                }
                out.writeInt(cm.fields().size());
                for (FieldMapping fm : cm.fields()) {
                    out.writeInt(pool.indexOf(fm.sourceDescriptor()));
                    for (int i = 0; i < namespaces.size(); i++) {
                        out.writeInt(pool.indexOf(nullToEmpty(fm.name(i))));
                    }
                }
                out.writeInt(cm.methods().size());
                for (MethodMapping mm : cm.methods()) {
                    out.writeInt(pool.indexOf(mm.sourceDescriptor()));
                    for (int i = 0; i < namespaces.size(); i++) {
                        out.writeInt(pool.indexOf(nullToEmpty(mm.name(i))));
                    }
                }
            }

            // Footer
            out.writeInt(CtgFormat.END_MARKER);
            out.flush();
            return Result.ok(null);
        } catch (IOException e) {
            return Result.err(new MappingError.IoError("<ctg-writer>", e.getMessage()));
        }
    }

    // =========================================================== string pool

    private static StringPoolBuilder collect(MappingTree tree) {
        StringPoolBuilder pool = new StringPoolBuilder();
        // Резервируем индекс 0 под "" (CtgFormat.EMPTY_STRING_INDEX).
        pool.add("");
        for (Namespace ns : tree.namespaces()) {
            pool.add(ns.name());
        }
        int nsCount = tree.namespaces().size();
        for (ClassMapping cm : tree.classes()) {
            for (int i = 0; i < nsCount; i++) {
                pool.add(nullToEmpty(cm.name(i)));
            }
            for (FieldMapping fm : cm.fields()) {
                pool.add(fm.sourceDescriptor());
                for (int i = 0; i < nsCount; i++) {
                    pool.add(nullToEmpty(fm.name(i)));
                }
            }
            for (MethodMapping mm : cm.methods()) {
                pool.add(mm.sourceDescriptor());
                for (int i = 0; i < nsCount; i++) {
                    pool.add(nullToEmpty(mm.name(i)));
                }
            }
        }
        return pool;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** Помощник: отображает строки на стабильные индексы. */
    private static final class StringPoolBuilder {
        private final Map<String, Integer> index = new HashMap<>(4096);
        private final List<String> ordered = new ArrayList<>(4096);

        void add(String s) {
            if (index.putIfAbsent(s, ordered.size()) == null) {
                ordered.add(s);
            }
        }

        int indexOf(String s) {
            Integer i = index.get(s);
            if (i == null) {
                throw new IllegalStateException("string not registered: " + s);
            }
            return i;
        }

        List<String> ordered() {
            return ordered;
        }
    }
}
