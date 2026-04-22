/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.manifest;

import dev.vida.core.ApiStatus;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Подраздел {@code vifada} в {@code vida.mod.json}.
 *
 * <p>Указывает, в каких пакетах искать Vifada-классы и какой JSON-файл описывает
 * их раскладку по стороне (client/server/common). Подробная схема
 * {@code ejemplo.vifada.json} — в модуле {@code :vifada}.
 */
@ApiStatus.Stable
public record VifadaConfig(
        List<String> packages,
        Optional<String> config,
        int priority) {

    /** Дефолтный приоритет Vifada-трансформаций. */
    public static final int DEFAULT_PRIORITY = 1000;

    /** Пустая секция для модов без Vifada. */
    public static final VifadaConfig EMPTY =
            new VifadaConfig(List.of(), Optional.empty(), DEFAULT_PRIORITY);

    public VifadaConfig {
        packages = List.copyOf(Objects.requireNonNull(packages, "packages"));
        Objects.requireNonNull(config, "config");
    }

    public boolean isEmpty() {
        return packages.isEmpty() && config.isEmpty();
    }
}
