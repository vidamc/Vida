/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery;

import dev.vida.core.ApiStatus;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * {@link ZipReader} над массивом байтов.
 *
 * <p>Использует последовательное чтение через {@link ZipInputStream}: все
 * записи распаковываются в память в конструкторе. Подходит для небольших
 * вложенных JAR (обычно ≤ 10 MB). Для больших архивов используйте
 * {@link FileZipReader}.
 */
@ApiStatus.Stable
public final class BytesZipReader implements ZipReader {

    private final String source;
    private final Map<String, byte[]> entries;

    public BytesZipReader(String source, byte[] data) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(data, "data");
        this.source = source;

        Map<String, byte[]> map = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                try {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    map.put(entry.getName(), zis.readAllBytes());
                } finally {
                    zis.closeEntry();
                }
            }
        }
        this.entries = Collections.unmodifiableMap(map);
    }

    @Override
    public String source() {
        return source;
    }

    @Override
    public boolean contains(String path) {
        return entries.containsKey(Objects.requireNonNull(path, "path"));
    }

    @Override
    public byte[] read(String path) throws IOException {
        byte[] bytes = entries.get(Objects.requireNonNull(path, "path"));
        if (bytes == null) {
            throw new FileNotFoundException("zip entry not found: " + path + " in " + source);
        }
        return bytes.clone();
    }

    @Override
    public InputStream open(String path) throws IOException {
        byte[] bytes = entries.get(Objects.requireNonNull(path, "path"));
        if (bytes == null) {
            throw new FileNotFoundException("zip entry not found: " + path + " in " + source);
        }
        return new ByteArrayInputStream(bytes);
    }

    @Override
    public Collection<String> entries() {
        return entries.keySet();
    }

    @Override
    public void close() {
        // no-op: байты держим до тех пор, пока жив объект
    }

    @Override
    public String toString() {
        return "BytesZipReader{" + source + ", entries=" + entries.size() + "}";
    }
}
