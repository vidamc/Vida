/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver.internal;

import dev.vida.core.ApiStatus;
import dev.vida.core.VersionRange;
import dev.vida.resolver.DependencyKind;
import dev.vida.resolver.Provider;

/**
 * Одно накопленное ограничение на id: кто его наложил, в каком диапазоне и
 * какого оно «характера» (hard/soft/incompat).
 *
 * <p>{@code declarer} — провайдер, наложивший ограничение; {@code null} для
 * корневых требований (которые не привязаны ни к какому моду).
 */
@ApiStatus.Internal
public record Constraint(String requester, VersionRange range, DependencyKind kind, Provider declarer) {
}
