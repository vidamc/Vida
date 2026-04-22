/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery;

import dev.vida.core.ApiStatus;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * {@link ZipReader} поверх {@link ZipFile}. Подходит для дисковых jar-файлов
 * произвольного размера: чтение одной записи идёт через central directory
 * без полного прохода по архиву.
 */
@ApiStatus.Stable
public final class FileZipReader implements ZipReader {

    private final String source;
    private final ZipFile zip;
    private volatile Collection<String> entryCache;

    public FileZipReader(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        this.source = path.toString();
        this.zip = new ZipFile(path.toFile(), ZipFile.OPEN_READ, StandardCharsets.UTF_8);
    }

    @Override
    public String source() {
        return source;
    }

    @Override
    public boolean contains(String path) {
        Objects.requireNonNull(path, "path");
        return zip.getEntry(path) != null;
    }

    @Override
    public byte[] read(String path) throws IOException {
        Objects.requireNonNull(path, "path");
        ZipEntry entry = zip.getEntry(path);
        if (entry == null) {
            throw new FileNotFoundException("zip entry not found: " + path + " in " + source);
        }
        long size = entry.getSize();
        int hint = size > 0 && size < (1 << 24) ? (int) size : 8192;
        try (InputStream in = zip.getInputStream(entry)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(hint);
            in.transferTo(out);
            return out.toByteArray();
        }
    }

    @Override
    public InputStream open(String path) throws IOException {
        Objects.requireNonNull(path, "path");
        ZipEntry entry = zip.getEntry(path);
        if (entry == null) {
            throw new FileNotFoundException("zip entry not found: " + path + " in " + source);
        }
        return zip.getInputStream(entry);
    }

    @Override
    public Collection<String> entries() {
        Collection<String> cached = entryCache;
        if (cached == null) {
            List<String> collected = new ArrayList<>();
            Enumeration<? extends ZipEntry> it = zip.entries();
            while (it.hasMoreElements()) {
                ZipEntry e = it.nextElement();
                if (!e.isDirectory()) {
                    collected.add(e.getName());
                }
            }
            cached = Collections.unmodifiableList(collected);
            entryCache = cached;
        }
        return cached;
    }

    @Override
    public void close() {
        try {
            zip.close();
        } catch (IOException ignored) {
            // контракт ZipReader.close() не бросает checked исключений
        }
    }

    @Override
    public String toString() {
        return "FileZipReader{" + source + "}";
    }
}
