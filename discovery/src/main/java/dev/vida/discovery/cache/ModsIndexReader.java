/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery.cache;

import dev.vida.core.ApiStatus;
import dev.vida.discovery.internal.Fingerprints;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Чтение {@link ModsIndex} из бинарного {@code mods.idx}.
 *
 * <p>Зеркалит {@link ModsIndexWriter}. При любой невалидности файла бросает
 * {@link IOException} — вызывающему следует обращаться с ним как со
 * штатным кэш-миссом и запустить полноценный скан.
 */
@ApiStatus.Stable
public final class ModsIndexReader {

    private ModsIndexReader() {}

    /** Читает индекс из файла. */
    public static ModsIndex readFile(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        try (InputStream in = Files.newInputStream(path)) {
            return read(in);
        }
    }

    /** Читает индекс из произвольного потока. Поток НЕ закрывается автоматически. */
    public static ModsIndex read(InputStream rawIn) throws IOException {
        Objects.requireNonNull(rawIn, "in");
        DataInputStream in = new DataInputStream(
                rawIn instanceof BufferedInputStream ? rawIn : new BufferedInputStream(rawIn));

        byte[] magic = new byte[ModsIndexFormat.MAGIC.length];
        in.readFully(magic);
        for (int i = 0; i < magic.length; i++) {
            if (magic[i] != ModsIndexFormat.MAGIC[i]) {
                throw new IOException("mods.idx: bad magic");
            }
        }
        int major = in.readUnsignedByte();
        int minor = in.readUnsignedByte();
        if (major != ModsIndexFormat.VERSION_MAJOR) {
            throw new IOException("mods.idx: unsupported version " + major + "." + minor);
        }
        in.readUnsignedShort(); // flags

        long writtenAtMillis = in.readLong();

        int poolSize = in.readInt();
        if (poolSize < 1 || poolSize > 10_000_000) {
            throw new IOException("mods.idx: bad string pool size " + poolSize);
        }
        String[] pool = new String[poolSize];
        for (int i = 0; i < poolSize; i++) {
            int len = in.readInt();
            if (len < 0 || len > 16 * 1024 * 1024) {
                throw new IOException("mods.idx: bad string length " + len);
            }
            byte[] bytes = new byte[len];
            in.readFully(bytes);
            pool[i] = new String(bytes, StandardCharsets.UTF_8);
        }
        if (!pool[ModsIndexFormat.EMPTY_STRING_INDEX].isEmpty()) {
            throw new IOException("mods.idx: string pool[0] must be empty");
        }

        int count = in.readInt();
        if (count < 0 || count > 10_000_000) {
            throw new IOException("mods.idx: bad entry count " + count);
        }
        List<ModsIndex.Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int sourceIdx = in.readInt();
            int depth = in.readInt();
            long mtime = in.readLong();
            long size = in.readLong();
            byte[] sha = new byte[Fingerprints.SHA256_LENGTH];
            in.readFully(sha);
            int modIdIdx = in.readInt();
            int modVerIdx = in.readInt();
            int nestedCount = in.readInt();
            if (nestedCount < 0 || nestedCount > 100_000) {
                throw new IOException("mods.idx: bad nested count " + nestedCount);
            }
            List<String> nested = new ArrayList<>(nestedCount);
            for (int n = 0; n < nestedCount; n++) {
                nested.add(poolGet(pool, in.readInt()));
            }
            entries.add(new ModsIndex.Entry(
                    poolGet(pool, sourceIdx),
                    depth,
                    mtime,
                    size,
                    sha,
                    poolGet(pool, modIdIdx),
                    poolGet(pool, modVerIdx),
                    nested));
        }

        int footer = in.readInt();
        if (footer != ModsIndexFormat.END_MARKER) {
            throw new IOException("mods.idx: bad end marker 0x" + Integer.toHexString(footer));
        }

        return new ModsIndex(Instant.ofEpochMilli(writtenAtMillis), entries);
    }

    private static String poolGet(String[] pool, int idx) throws IOException {
        if (idx < 0 || idx >= pool.length) {
            throw new IOException("mods.idx: bad string index " + idx);
        }
        return pool[idx];
    }
}
