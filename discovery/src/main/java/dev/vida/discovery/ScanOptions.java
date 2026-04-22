/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery;

import dev.vida.core.ApiStatus;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Параметры {@link ModScanner}.
 *
 * <p>Экземпляры иммутабельны: каждый сеттер возвращает новый объект. Это
 * удобно для локального переопределения одной опции без мутации общего
 * шаблона.
 *
 * <p>Значения по умолчанию:
 * <ul>
 *   <li>{@code manifestPath} — {@value #DEFAULT_MANIFEST_PATH};</li>
 *   <li>{@code extensions}   — {@code .jar};</li>
 *   <li>{@code maxNestingDepth} — {@value #DEFAULT_MAX_NESTING};</li>
 *   <li>{@code followNested}  — {@code true};</li>
 *   <li>{@code computeFingerprints} — {@code true}.</li>
 * </ul>
 */
@ApiStatus.Stable
public final class ScanOptions {

    public static final String DEFAULT_MANIFEST_PATH = "vida.mod.json";
    public static final int DEFAULT_MAX_NESTING = 4;

    private static final Set<String> DEFAULT_EXTENSIONS = Set.of(".jar");

    private static final ScanOptions DEFAULTS = new ScanOptions(
            DEFAULT_MANIFEST_PATH,
            DEFAULT_EXTENSIONS,
            DEFAULT_MAX_NESTING,
            true,
            true);

    private final String manifestPath;
    private final Set<String> extensions;
    private final int maxNestingDepth;
    private final boolean followNested;
    private final boolean computeFingerprints;

    private ScanOptions(
            String manifestPath,
            Set<String> extensions,
            int maxNestingDepth,
            boolean followNested,
            boolean computeFingerprints) {
        this.manifestPath = manifestPath;
        this.extensions = extensions;
        this.maxNestingDepth = maxNestingDepth;
        this.followNested = followNested;
        this.computeFingerprints = computeFingerprints;
    }

    /** Настройки по умолчанию. */
    public static ScanOptions defaults() {
        return DEFAULTS;
    }

    // ---- accessors ----

    public String manifestPath() { return manifestPath; }
    public Set<String> extensions() { return extensions; }
    public int maxNestingDepth() { return maxNestingDepth; }
    public boolean followNested() { return followNested; }
    public boolean computeFingerprints() { return computeFingerprints; }

    // ---- with-style copiers ----

    public ScanOptions withManifestPath(String path) {
        Objects.requireNonNull(path, "path");
        if (path.isBlank()) throw new IllegalArgumentException("manifestPath is blank");
        return new ScanOptions(path, extensions, maxNestingDepth, followNested, computeFingerprints);
    }

    public ScanOptions withExtensions(String... exts) {
        Objects.requireNonNull(exts, "exts");
        Set<String> set = new LinkedHashSet<>();
        for (String e : exts) {
            if (e == null || e.isBlank()) continue;
            String norm = e.startsWith(".") ? e : "." + e;
            set.add(norm.toLowerCase(Locale.ROOT));
        }
        if (set.isEmpty()) {
            throw new IllegalArgumentException("at least one extension required");
        }
        return new ScanOptions(manifestPath, Collections.unmodifiableSet(set),
                maxNestingDepth, followNested, computeFingerprints);
    }

    public ScanOptions withMaxNestingDepth(int depth) {
        if (depth < 0) throw new IllegalArgumentException("depth must be >= 0");
        return new ScanOptions(manifestPath, extensions, depth, followNested, computeFingerprints);
    }

    public ScanOptions withFollowNested(boolean follow) {
        return new ScanOptions(manifestPath, extensions, maxNestingDepth, follow, computeFingerprints);
    }

    public ScanOptions withComputeFingerprints(boolean compute) {
        return new ScanOptions(manifestPath, extensions, maxNestingDepth, followNested, compute);
    }

    @Override
    public String toString() {
        return "ScanOptions{manifest=" + manifestPath
                + ", extensions=" + extensions
                + ", maxNesting=" + maxNestingDepth
                + ", followNested=" + followNested
                + ", fingerprints=" + computeFingerprints + "}";
    }

    // ---- equality — рекордоподобная по полям ----

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScanOptions that)) return false;
        return maxNestingDepth == that.maxNestingDepth
                && followNested == that.followNested
                && computeFingerprints == that.computeFingerprints
                && manifestPath.equals(that.manifestPath)
                && extensions.equals(that.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(manifestPath, extensions, maxNestingDepth, followNested, computeFingerprints);
    }
}
