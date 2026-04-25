/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import dev.vida.core.ApiStatus;

/**
 * Дескриптор подписки, возвращаемый {@link LatidoBus#suscribir}.
 *
 * <p>Вызов {@link #cancelar()} удаляет подписчика из шины. Повторные
 * вызовы — no-op. {@link #activa()} позволяет проверить, жива ли
 * подписка.
 *
 * <p>Реализация {@link AutoCloseable} — чтобы удобно было писать
 * try-with-resources в сценариях «подписаться только на время этой
 * операции».
 */
@ApiStatus.Stable
public interface Suscripcion extends AutoCloseable {

    /** {@code true}, если подписка ещё жива (не отменена). */
    boolean activa();

    /** Отменяет подписку. Идемпотентно. */
    void cancelar();

    /** Псевдоним {@link #cancelar()} для try-with-resources. */
    @Override
    default void close() {
        cancelar();
    }
}
