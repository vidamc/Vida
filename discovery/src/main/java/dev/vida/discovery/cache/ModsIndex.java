/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery.cache;

import dev.vida.core.ApiStatus;
import dev.vida.discovery.DiscoveryReport;
import dev.vida.discovery.ModCandidate;
import dev.vida.discovery.ModSource;
import dev.vida.discovery.internal.Fingerprints;
import dev.vida.discovery.internal.NestedJars;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Иммутабельный снимок состояния {@code mods/} для быстрого повторного старта.
 *
 * <p>Записывается в {@code mods.idx} рядом с директорией модов. На следующем
 * старте loader сравнивает {@code size}+{@code mtime} каждого файла с записью
 * в кэше; совпадение означает, что манифест перечитывать не нужно.
 */
@ApiStatus.Stable
public final class ModsIndex {

    private final Instant writtenAt;
    private final List<Entry> entries;
    private final Map<String, Entry> bySourceId;

    public ModsIndex(Instant writtenAt, List<Entry> entries) {
        Objects.requireNonNull(writtenAt, "writtenAt");
        Objects.requireNonNull(entries, "entries");
        this.writtenAt = writtenAt;
        this.entries = List.copyOf(entries);

        Map<String, Entry> idx = new HashMap<>(this.entries.size() * 2);
        for (Entry e : this.entries) {
            idx.put(e.sourceId(), e);
        }
        this.bySourceId = Collections.unmodifiableMap(idx);
    }

    public Instant writtenAt() { return writtenAt; }

    public List<Entry> entries() { return entries; }

    public int size() { return entries.size(); }

    /** Запись по {@link Entry#sourceId()} или {@code null}. */
    public Entry get(String sourceId) {
        return bySourceId.get(Objects.requireNonNull(sourceId, "sourceId"));
    }

    /**
     * Снимок из результата скана. Добавляет mtime для {@link ModSource.OnDisk}
     * (для {@link ModSource.Embedded} — 0) и использует
     * {@link ModSource#sizeBytes()} для {@code sizeBytes}.
     */
    public static ModsIndex capture(DiscoveryReport report) {
        Objects.requireNonNull(report, "report");
        List<Entry> out = new ArrayList<>(report.all().size());
        for (ModCandidate c : report.all()) {
            long mtime = 0L;
            if (c.source() instanceof ModSource.OnDisk od) {
                try {
                    mtime = Files.getLastModifiedTime(od.path()).toMillis();
                } catch (IOException ignored) {
                    mtime = 0L;
                }
            }
            long size = Math.max(0L, c.source().sizeBytes());
            out.add(new Entry(
                    c.source().id(),
                    c.depth(),
                    mtime,
                    size,
                    c.sha256(),
                    c.manifest().id(),
                    c.manifest().version().toString(),
                    NestedJars.from(c.manifest())));
        }
        return new ModsIndex(Instant.now(), out);
    }

    @Override
    public String toString() {
        return "ModsIndex{writtenAt=" + writtenAt + ", entries=" + entries.size() + "}";
    }

    // =============================================================== Entry

    /**
     * Запись о одном кандидате (top-level или вложенном).
     *
     * @param sourceId           стабильный идентификатор источника (см. {@link ModSource#id()})
     * @param depth              глубина вложенности (0 — top-level)
     * @param mtimeMillis        время модификации файла (0 для вложенных)
     * @param sizeBytes          размер архива в байтах
     * @param sha256             SHA-256 архива (32 байта)
     * @param modId              id мода из манифеста
     * @param modVersion         строковое представление версии
     * @param nestedInnerPaths   объявленные вложенные пути (для валидации структуры)
     */
    public record Entry(
            String sourceId,
            int depth,
            long mtimeMillis,
            long sizeBytes,
            byte[] sha256,
            String modId,
            String modVersion,
            List<String> nestedInnerPaths) {

        public Entry {
            Objects.requireNonNull(sourceId, "sourceId");
            Objects.requireNonNull(modId, "modId");
            Objects.requireNonNull(modVersion, "modVersion");
            Objects.requireNonNull(sha256, "sha256");
            Objects.requireNonNull(nestedInnerPaths, "nestedInnerPaths");
            if (sha256.length != Fingerprints.SHA256_LENGTH) {
                throw new IllegalArgumentException(
                        "sha256 must be " + Fingerprints.SHA256_LENGTH + " bytes");
            }
            sha256 = sha256.clone();
            nestedInnerPaths = List.copyOf(nestedInnerPaths);
        }

        /** Защитная копия отпечатка. */
        @Override
        public byte[] sha256() {
            return sha256.clone();
        }

        /** Hex-представление отпечатка. */
        public String sha256Hex() {
            return Fingerprints.hex(sha256);
        }
    }
}
