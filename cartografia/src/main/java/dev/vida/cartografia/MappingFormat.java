/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia;

import dev.vida.core.ApiStatus;

/**
 * Известные форматы мэппингов.
 *
 * <p>Cartografía читает и пишет:
 * <ul>
 *   <li>{@link #PROGUARD} — Mojang-овский текстовый proguard-формат;</li>
 *   <li>{@link #CTG} — собственный компактный бинарный формат Vida.</li>
 * </ul>
 *
 * <p>Tiny v2, Enigma и прочие планируются позже и будут добавлены сюда без
 * breaking-изменений (enum расширяется).
 */
@ApiStatus.Stable
public enum MappingFormat {
    /** Mojang proguard: текстовый, namespace {@code named → obf}. */
    PROGUARD("proguard", ".txt"),

    /** Vida CarToGrafía: бинарный, N namespace, string-pool. */
    CTG("ctg", ".ctg");

    private final String id;
    private final String defaultExtension;

    MappingFormat(String id, String defaultExtension) {
        this.id = id;
        this.defaultExtension = defaultExtension;
    }

    /** Короткий идентификатор формата, стабильный между версиями. */
    public String id() {
        return id;
    }

    /** Типовое расширение файла для этого формата (с точкой). */
    public String defaultExtension() {
        return defaultExtension;
    }
}
