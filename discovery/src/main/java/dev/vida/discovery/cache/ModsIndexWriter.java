/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery.cache;

import dev.vida.core.ApiStatus;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Запись {@link ModsIndex} в бинарный {@code mods.idx}.
 *
 * <p>Запись на диск — атомарная: сначала во временный файл, затем
 * {@link StandardCopyOption#ATOMIC_MOVE} на целевой путь.
 */
@ApiStatus.Stable
public final class ModsIndexWriter {

    private ModsIndexWriter() {}

    /**
     * Пишет индекс в произвольный поток (поток НЕ закрывается).
     */
    public static void write(ModsIndex index, OutputStream rawOut) throws IOException {
        Objects.requireNonNull(index, "index");
        Objects.requireNonNull(rawOut, "out");

        StringPoolBuilder pool = collect(index);

        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(rawOut));
        out.write(ModsIndexFormat.MAGIC);
        out.writeByte(ModsIndexFormat.VERSION_MAJOR);
        out.writeByte(ModsIndexFormat.VERSION_MINOR);
        out.writeShort(0);
        out.writeLong(index.writtenAt().toEpochMilli());

        List<String> strings = pool.ordered();
        out.writeInt(strings.size());
        for (String s : strings) {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            out.writeInt(bytes.length);
            out.write(bytes);
        }

        out.writeInt(index.size());
        for (ModsIndex.Entry e : index.entries()) {
            out.writeInt(pool.indexOf(e.sourceId()));
            out.writeInt(e.depth());
            out.writeLong(e.mtimeMillis());
            out.writeLong(e.sizeBytes());
            out.write(e.sha256());
            out.writeInt(pool.indexOf(e.modId()));
            out.writeInt(pool.indexOf(e.modVersion()));
            out.writeInt(e.nestedInnerPaths().size());
            for (String inner : e.nestedInnerPaths()) {
                out.writeInt(pool.indexOf(inner));
            }
        }

        out.writeInt(ModsIndexFormat.END_MARKER);
        out.flush();
    }

    /** Атомарная запись на файловую систему. */
    public static void writeAtomic(ModsIndex index, Path target) throws IOException {
        Objects.requireNonNull(target, "target");
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = (parent == null ? target.getFileSystem().getPath(".") : parent)
                .resolve(target.getFileName().toString() + ".tmp");
        try (OutputStream os = Files.newOutputStream(
                tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            write(index, os);
        }
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // ATOMIC_MOVE может не поддерживаться FS — fallback.
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ==================================================================

    private static StringPoolBuilder collect(ModsIndex index) {
        StringPoolBuilder pool = new StringPoolBuilder();
        pool.add(""); // резервируем пустой индекс
        for (ModsIndex.Entry e : index.entries()) {
            pool.add(e.sourceId());
            pool.add(e.modId());
            pool.add(e.modVersion());
            for (String inner : e.nestedInnerPaths()) {
                pool.add(inner);
            }
        }
        return pool;
    }

    private static final class StringPoolBuilder {
        private final Map<String, Integer> index = new HashMap<>(256);
        private final List<String> ordered = new ArrayList<>(256);

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
