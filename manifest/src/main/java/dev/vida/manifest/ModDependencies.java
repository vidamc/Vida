/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.manifest;

import dev.vida.core.ApiStatus;
import dev.vida.core.VersionRange;
import java.util.Map;
import java.util.Objects;

/**
 * Блок {@code dependencies} манифеста: жёсткие, мягкие и несовместимые зависимости.
 *
 * <p>Ключ — id мода (или зарезервированное имя, например {@code vida-loader},
 * {@code minecraft}); значение — диапазон версий SemVer.
 */
@ApiStatus.Stable
public record ModDependencies(
        Map<String, VersionRange> required,
        Map<String, VersionRange> optional,
        Map<String, VersionRange> incompatibilities) {

    public static final ModDependencies EMPTY =
            new ModDependencies(Map.of(), Map.of(), Map.of());

    public ModDependencies {
        required          = Map.copyOf(Objects.requireNonNull(required, "required"));
        optional          = Map.copyOf(Objects.requireNonNull(optional, "optional"));
        incompatibilities = Map.copyOf(Objects.requireNonNull(incompatibilities, "incompatibilities"));
    }

    public boolean isEmpty() {
        return required.isEmpty() && optional.isEmpty() && incompatibilities.isEmpty();
    }
}
