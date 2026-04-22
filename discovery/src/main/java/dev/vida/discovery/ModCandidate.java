/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery;

import dev.vida.core.ApiStatus;
import dev.vida.discovery.internal.Fingerprints;
import dev.vida.manifest.ModManifest;
import java.util.List;
import java.util.Objects;

/**
 * Отдельный обнаруженный мод-кандидат.
 *
 * <p>Содержит только метаданные: источник, разобранный манифест, отпечаток
 * архива и список вложенных кандидатов. Байт-код jar-файла не удерживается в
 * памяти сверх того, что требуется {@link ModSource}.
 *
 * <p>Для повторного доступа к содержимому архива используйте
 * {@link ModSource#open()}.
 */
@ApiStatus.Stable
public final class ModCandidate {

    private final ModSource source;
    private final ModManifest manifest;
    private final byte[] sha256;
    private final List<ModCandidate> nested;
    private final int depth;

    public ModCandidate(
            ModSource source,
            ModManifest manifest,
            byte[] sha256,
            List<ModCandidate> nested,
            int depth) {
        this.source = Objects.requireNonNull(source, "source");
        this.manifest = Objects.requireNonNull(manifest, "manifest");
        this.sha256 = Objects.requireNonNull(sha256, "sha256").clone();
        if (this.sha256.length != Fingerprints.SHA256_LENGTH) {
            throw new IllegalArgumentException(
                    "sha256 must be " + Fingerprints.SHA256_LENGTH + " bytes, got " + this.sha256.length);
        }
        this.nested = List.copyOf(nested);
        if (depth < 0) {
            throw new IllegalArgumentException("depth < 0");
        }
        this.depth = depth;
    }

    /** Источник (файл на диске или вложенный архив). */
    public ModSource source() {
        return source;
    }

    /** Разобранный манифест мода. */
    public ModManifest manifest() {
        return manifest;
    }

    /**
     * SHA-256 содержимого архива. Массив — защитная копия, безопасно
     * модифицировать вызывающему.
     */
    public byte[] sha256() {
        return sha256.clone();
    }

    /** Hex-представление SHA-256 (стабильно для логов/кэша). */
    public String sha256Hex() {
        return Fingerprints.hex(sha256);
    }

    /** Список вложенных jar-кандидатов (может быть пустым). */
    public List<ModCandidate> nested() {
        return nested;
    }

    /** Глубина вложенности: 0 — top-level, 1+ — встроенный. */
    public int depth() {
        return depth;
    }

    /** Удобный shortcut: mod id из манифеста. */
    public String id() {
        return manifest.id();
    }

    @Override
    public String toString() {
        return "ModCandidate{" + manifest.id() + "@" + manifest.version()
                + ", source=" + source.id()
                + ", depth=" + depth
                + ", nested=" + nested.size() + "}";
    }
}
