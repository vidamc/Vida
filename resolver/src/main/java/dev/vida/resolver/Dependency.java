/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import dev.vida.core.ApiStatus;
import dev.vida.core.VersionRange;
import java.util.Objects;

/**
 * Одна запись в списке зависимостей {@link Provider}.
 *
 * @param targetId id целевого мода (совпадает либо с {@code id} провайдера,
 *                 либо с одной из его строк в {@code provides})
 * @param range    допустимый диапазон версий
 * @param kind     тип зависимости — см. {@link DependencyKind}
 */
@ApiStatus.Stable
public record Dependency(String targetId, VersionRange range, DependencyKind kind) {

    public Dependency {
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(kind, "kind");
        if (targetId.isBlank()) {
            throw new IllegalArgumentException("targetId must not be blank");
        }
    }

    public static Dependency required(String id, VersionRange range) {
        return new Dependency(id, range, DependencyKind.REQUIRED);
    }

    public static Dependency optional(String id, VersionRange range) {
        return new Dependency(id, range, DependencyKind.OPTIONAL);
    }

    public static Dependency incompatible(String id, VersionRange range) {
        return new Dependency(id, range, DependencyKind.INCOMPATIBLE);
    }

    @Override
    public String toString() {
        return kind.name().toLowerCase() + ":" + targetId + "@" + range;
    }
}
