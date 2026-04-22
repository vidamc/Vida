/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Рантайм Vida: Java-агент, трансформер байткода и иерархия
 * {@link ClassLoader}'ов.
 *
 * <h2>Точки входа</h2>
 * <ul>
 *   <li>{@link dev.vida.loader.VidaPremain} — входные точки
 *       {@code premain}/{@code agentmain} для запуска под
 *       {@code -javaagent:vida-loader.jar}. Делегирует в
 *       {@link dev.vida.loader.VidaBoot}.</li>
 *   <li>{@link dev.vida.loader.VidaBoot} — программная инициализация
 *       Vida без участия JVM-агента (тесты, инструменты).</li>
 * </ul>
 *
 * <h2>Последовательность запуска</h2>
 * <ol>
 *   <li>Парсим {@link dev.vida.loader.BootOptions}.</li>
 *   <li>Сканируем директорию модов через {@code :discovery}.</li>
 *   <li>Читаем манифесты через {@code :manifest}.</li>
 *   <li>Резолвим зависимости через {@code :resolver}.</li>
 *   <li>Собираем {@link dev.vida.loader.MorphIndex} — мапу
 *       {@code target → List<MorphSource>}.</li>
 *   <li>Регистрируем {@link dev.vida.loader.VidaClassTransformer} в
 *       {@code Instrumentation} либо через кастомный
 *       {@link dev.vida.loader.TransformingClassLoader}.</li>
 *   <li>Передаём управление игре / возвращаемся из {@code premain}.</li>
 * </ol>
 *
 * <h2>Иерархия классов</h2>
 * <pre>
 *   System / Agent  (vida-loader + зависимости)
 *        │
 *        ▼
 *   JuegoLoader     (игра + Vida API)
 *        │
 *        ▼
 *   ModLoader × N   (каждый мод — свой, parent = JuegoLoader)
 * </pre>
 */
@ApiStatus.Preview("loader")
package dev.vida.loader;

import dev.vida.core.ApiStatus;
