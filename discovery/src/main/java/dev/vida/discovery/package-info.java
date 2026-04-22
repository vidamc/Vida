/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Vida mod-discovery.
 *
 * <p>Публичные типы:
 * <ul>
 *   <li>{@link dev.vida.discovery.ZipReader} — абстракция над zip/jar с
 *       двумя реализациями: {@link dev.vida.discovery.FileZipReader} для
 *       дисковых файлов и {@link dev.vida.discovery.BytesZipReader} для
 *       in-memory вложенных архивов;</li>
 *   <li>{@link dev.vida.discovery.ModSource} — sealed-иерархия источников
 *       ({@link dev.vida.discovery.ModSource.OnDisk},
 *       {@link dev.vida.discovery.ModSource.Embedded});</li>
 *   <li>{@link dev.vida.discovery.ModCandidate} — разобранный манифест +
 *       источник + отпечаток содержимого;</li>
 *   <li>{@link dev.vida.discovery.ModScanner} — обход директории и
 *       рекурсивное разворачивание вложенных JAR;</li>
 *   <li>{@link dev.vida.discovery.DiscoveryReport} — сгруппированный
 *       результат скана (успехи + ошибки + статистика);</li>
 *   <li>{@link dev.vida.discovery.DiscoveryError} — sealed-иерархия ошибок.</li>
 * </ul>
 *
 * <p>Пакет {@code cache} — собственный бинарный формат {@code mods.idx} для
 * пропуска повторного разбора манифестов при неизменных файлах.
 */
package dev.vida.discovery;
