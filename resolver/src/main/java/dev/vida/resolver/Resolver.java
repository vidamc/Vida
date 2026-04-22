/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import dev.vida.resolver.internal.Backtracker;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Публичная точка входа в резолвер зависимостей.
 *
 * <p>Типовой сценарий использования:
 * <pre>{@code
 * Universe u = Universe.builder()
 *         .add(modA) // Provider’ы из дискавери или тестов
 *         .add(modB)
 *         .build();
 *
 * Result<Resolution, ResolverError> r = Resolver.resolve(
 *         Set.of("fabric-api", "sodium"), u, ResolverOptions.DEFAULTS);
 * }</pre>
 *
 * Все методы thread-safe в том смысле, что не разделяют состояние между вызовами.
 */
@ApiStatus.Stable
public final class Resolver {

    private Resolver() {}

    /** То же, что {@link #resolve(Set, Universe, ResolverOptions)} c {@link ResolverOptions#DEFAULTS}. */
    public static Result<Resolution, ResolverError> resolve(Set<String> roots, Universe universe) {
        return resolve(roots, universe, ResolverOptions.DEFAULTS);
    }

    /**
     * Запускает резолвер.
     *
     * @param roots    корневые id — моды, присутствие которых обязательно
     *                 (обычно — всё, что было физически найдено в {@code mods/})
     * @param universe каталог всех кандидатов
     * @param options  настройки стратегии и лимиты
     */
    public static Result<Resolution, ResolverError> resolve(
            Set<String> roots, Universe universe, ResolverOptions options) {
        Objects.requireNonNull(roots, "roots");
        Objects.requireNonNull(universe, "universe");
        Objects.requireNonNull(options, "options");

        // Sanitizing: выкидываем пустые, защищаемся от null внутри.
        Set<String> cleanRoots = new LinkedHashSet<>(roots.size());
        for (String r : roots) {
            if (r == null || r.isBlank()) {
                throw new IllegalArgumentException("roots must not contain null or blank ids");
            }
            cleanRoots.add(r);
        }

        Backtracker bt = new Backtracker(universe, options);
        return bt.solve(cleanRoots);
    }
}
