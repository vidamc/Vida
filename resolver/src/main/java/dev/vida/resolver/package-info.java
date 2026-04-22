/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Резолвер зависимостей модов Vida.
 *
 * <h2>Обзор</h2>
 * Резолвер принимает на вход:
 * <ul>
 *   <li>{@link dev.vida.resolver.Universe} — каталог всех известных провайдеров
 *       (по одному на каждую пару {@code (id, version)}), построенный из
 *       дискавери или тестовых фикстур;</li>
 *   <li>множество <i>корневых</i> id — модов, присутствие которых обязательно
 *       (обычно — всё, что лежит в {@code mods/});</li>
 *   <li>{@link dev.vida.resolver.ResolverOptions} — стратегия выбора, pin’ы,
 *       лимиты и т.д.</li>
 * </ul>
 *
 * Выход — {@link dev.vida.core.Result} с {@link dev.vida.resolver.Resolution}
 * при успехе и {@link dev.vida.resolver.ResolverError} при неудаче.
 *
 * <h2>Модель</h2>
 * <ul>
 *   <li>{@link dev.vida.resolver.Provider} — мод, которого можно выбрать;
 *       хранит id, {@link dev.vida.core.Version}, список {@code provides}
 *       (аliases) и зависимости.</li>
 *   <li>{@link dev.vida.resolver.Dependency} — одна запись зависимости:
 *       {@code (targetId, range, kind)}.</li>
 *   <li>{@link dev.vida.resolver.DependencyKind} — REQUIRED / OPTIONAL /
 *       INCOMPATIBLE.</li>
 * </ul>
 *
 * <h2>Алгоритм</h2>
 * Классический хронологический бэктрекинг с unit-пропагацией диапазонов:
 * каждое решение расширяет множество активных ограничений; при противоречии
 * резолвер откатывает последнее решение и пробует следующий кандидат.
 * Лимиты по времени и количеству решений защищают от патологических входов.
 *
 * <h2>Адаптер к манифестам</h2>
 * {@link dev.vida.resolver.ManifestAdapter} собирает {@link dev.vida.resolver.Provider}
 * из {@link dev.vida.manifest.ModManifest}, так что на практике использование
 * сводится к паре вызовов.
 */
@ApiStatus.Stable
package dev.vida.resolver;

import dev.vida.core.ApiStatus;
