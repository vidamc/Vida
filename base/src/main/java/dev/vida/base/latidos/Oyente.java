/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import dev.vida.core.ApiStatus;

/**
 * Слушатель события (подписчик).
 *
 * <p>Тип объявлен как {@code @FunctionalInterface}, чтобы можно было
 * передавать лямбды и method references: {@code bus.suscribir(TIPO,
 * Prioridad.NORMAL, e -> log.info(e.toString()))}.
 *
 * <p>Исключения, брошенные {@link #manejar(Object)}, шина перехватывает и
 * <b>не</b> пробрасывает дальше: сбой одного подписчика не должен валить
 * другие. Ошибка логируется через Vida-логгер; счётчик ошибок доступен
 * в {@link LatidoDespacho#errores()}.
 *
 * @param <E> тип события
 */
@FunctionalInterface
@ApiStatus.Stable
public interface Oyente<E> {

    /**
     * Обрабатывает событие.
     *
     * @param evento экземпляр события (никогда не {@code null})
     * @throws Exception произвольные ошибки — ловятся шиной
     */
    void manejar(E evento) throws Exception;
}
