/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia.io;

import dev.vida.cartografia.MappingError;
import dev.vida.cartografia.MappingTree;
import dev.vida.cartografia.Namespace;
import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Чтение бинарного формата {@link CtgFormat .ctg} в {@link MappingTree}.
 *
 * <p>Зеркалит {@link CtgWriter}. Возвращает {@link Result.Err} при:
 * <ul>
 *   <li>неверной сигнатуре ({@link MappingError.Corrupted});</li>
 *   <li>неподдерживаемой версии ({@link MappingError.UnsupportedVersion});</li>
 *   <li>обрезанном/некорректном потоке ({@link MappingError.Corrupted});</li>
 *   <li>произвольной I/O-ошибке ({@link MappingError.IoError}).</li>
 * </ul>
 */
@ApiStatus.Stable
public final class CtgReader {

    private CtgReader() {}

    /**
     * Читает дерево. Поток НЕ закрывается автоматически.
     *
     * @param sourceName имя источника для ошибок
     * @param in поток, позиционированный на первый байт {@code .ctg}
     */
    public static Result<MappingTree, MappingError> read(String sourceName, InputStream in) {
        Objects.requireNonNull(sourceName, "sourceName");
        Objects.requireNonNull(in, "in");

        DataInputStream dis = new DataInputStream(
                in instanceof BufferedInputStream ? in : new BufferedInputStream(in));

        try {
            // Header
            byte[] magic = new byte[CtgFormat.MAGIC.length];
            dis.readFully(magic);
            for (int i = 0; i < magic.length; i++) {
                if (magic[i] != CtgFormat.MAGIC[i]) {
                    return Result.err(new MappingError.Corrupted(sourceName, "bad magic"));
                }
            }
            int major = dis.readUnsignedByte();
            int minor = dis.readUnsignedByte();
            if (major != CtgFormat.VERSION_MAJOR) {
                return Result.err(new MappingError.UnsupportedVersion(sourceName, major, minor));
            }
            dis.readUnsignedShort(); // flags, ignored

            // String pool
            int poolSize = dis.readInt();
            if (poolSize < 0 || poolSize > 10_000_000) {
                return Result.err(new MappingError.Corrupted(sourceName, "string pool count=" + poolSize));
            }
            String[] pool = new String[poolSize];
            for (int i = 0; i < poolSize; i++) {
                int len = dis.readInt();
                if (len < 0 || len > 16 * 1024 * 1024) {
                    return Result.err(new MappingError.Corrupted(sourceName, "string length=" + len));
                }
                byte[] bytes = new byte[len];
                dis.readFully(bytes);
                pool[i] = new String(bytes, StandardCharsets.UTF_8);
            }
            if (poolSize == 0 || !pool[CtgFormat.EMPTY_STRING_INDEX].isEmpty()) {
                return Result.err(new MappingError.Corrupted(
                        sourceName, "string pool must start with empty string"));
            }

            // Namespaces
            int nsCount = dis.readUnsignedShort();
            if (nsCount < 1) {
                return Result.err(new MappingError.Corrupted(sourceName, "no namespaces"));
            }
            Namespace[] namespaces = new Namespace[nsCount];
            for (int i = 0; i < nsCount; i++) {
                int nameIdx = dis.readInt();
                if (!inRange(nameIdx, pool.length)) {
                    return Result.err(new MappingError.Corrupted(
                            sourceName, "bad ns string index " + nameIdx));
                }
                namespaces[i] = Namespace.of(pool[nameIdx]);
            }

            MappingTree.Builder tb = MappingTree.builder(
                    namespaces[0],
                    java.util.Arrays.copyOfRange(namespaces, 1, nsCount));

            // Classes
            int classCount = dis.readInt();
            if (classCount < 0 || classCount > 10_000_000) {
                return Result.err(new MappingError.Corrupted(sourceName, "class count=" + classCount));
            }
            for (int c = 0; c < classCount; c++) {
                String[] classNames = new String[nsCount];
                for (int i = 0; i < nsCount; i++) {
                    int idx = dis.readInt();
                    if (!inRange(idx, pool.length)) {
                        return Result.err(new MappingError.Corrupted(
                                sourceName, "bad class name index " + idx));
                    }
                    classNames[i] = pool[idx];
                }
                MappingTree.ClassBuilder cb;
                try {
                    cb = tb.addClass(classNames);
                } catch (IllegalStateException dup) {
                    return Result.err(new MappingError.DuplicateEntry(
                            classNames[0], namespaces[0].name()));
                }

                int fieldCount = dis.readInt();
                if (fieldCount < 0 || fieldCount > 1_000_000) {
                    return Result.err(new MappingError.Corrupted(
                            sourceName, "field count=" + fieldCount));
                }
                for (int f = 0; f < fieldCount; f++) {
                    int descIdx = dis.readInt();
                    if (!inRange(descIdx, pool.length)) {
                        return Result.err(new MappingError.Corrupted(
                                sourceName, "bad field desc index " + descIdx));
                    }
                    String desc = pool[descIdx];
                    String[] names = new String[nsCount];
                    for (int i = 0; i < nsCount; i++) {
                        int idx = dis.readInt();
                        if (!inRange(idx, pool.length)) {
                            return Result.err(new MappingError.Corrupted(
                                    sourceName, "bad field name index " + idx));
                        }
                        names[i] = pool[idx];
                    }
                    cb.addField(desc, names);
                }

                int methodCount = dis.readInt();
                if (methodCount < 0 || methodCount > 1_000_000) {
                    return Result.err(new MappingError.Corrupted(
                            sourceName, "method count=" + methodCount));
                }
                for (int m = 0; m < methodCount; m++) {
                    int descIdx = dis.readInt();
                    if (!inRange(descIdx, pool.length)) {
                        return Result.err(new MappingError.Corrupted(
                                sourceName, "bad method desc index " + descIdx));
                    }
                    String desc = pool[descIdx];
                    String[] names = new String[nsCount];
                    for (int i = 0; i < nsCount; i++) {
                        int idx = dis.readInt();
                        if (!inRange(idx, pool.length)) {
                            return Result.err(new MappingError.Corrupted(
                                    sourceName, "bad method name index " + idx));
                        }
                        names[i] = pool[idx];
                    }
                    cb.addMethod(desc, names);
                }
                cb.done();
            }

            int footer = dis.readInt();
            if (footer != CtgFormat.END_MARKER) {
                return Result.err(new MappingError.Corrupted(
                        sourceName, "bad end marker 0x" + Integer.toHexString(footer)));
            }
            return Result.ok(tb.build());
        } catch (IOException e) {
            return Result.err(new MappingError.IoError(sourceName, e.getMessage()));
        }
    }

    private static boolean inRange(int idx, int size) {
        return idx >= 0 && idx < size;
    }
}
