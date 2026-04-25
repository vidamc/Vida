/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Управляемый thread-pool Vida.
 *
 * <p>Задача модуля — дать модам простой, детерминированный способ
 * исполнять код вне main-тика без ручной работы с
 * {@link java.util.concurrent.ExecutorService} и без риска подружиться
 * с Minecraft-threading багами.
 *
 * <h2>Основные типы</h2>
 * <ul>
 *   <li>{@link dev.vida.susurro.Susurro} — facade: пускает задачи,
 *       контролирует back-pressure, интегрируется с main-тиком.</li>
 *   <li>{@link dev.vida.susurro.Tarea} — future + отмена + deadline.</li>
 *   <li>{@link dev.vida.susurro.Prioridad} — ALTA / NORMAL / BAJA.</li>
 *   <li>{@link dev.vida.susurro.HiloPrincipal} — очередь callbacks,
 *       обрабатываемая синхронно на main-тике через {@code pulso()}.</li>
 * </ul>
 *
 * <p>Поведение детерминировано: задачи одинакового приоритета выполняются
 * в порядке поступления (FIFO); внутри пула workers ≤ CPU-count по
 * умолчанию. Back-pressure — через {@link dev.vida.susurro.Susurro.Politica}.
 */
@ApiStatus.Stable
package dev.vida.susurro;

import dev.vida.core.ApiStatus;
