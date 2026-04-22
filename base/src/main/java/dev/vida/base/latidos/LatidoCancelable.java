/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import dev.vida.core.ApiStatus;

/**
 * Маркер отменяемого события.
 *
 * <p>Событие, чей тип реализует {@code LatidoCancelable}, может быть отменено
 * любым подписчиком с {@link Prioridad#URGENTE URGENTE}…{@link Prioridad#BAJA
 * BAJA}. После вызова {@link #cancelar()} шина продолжает доставку
 * оставшимся подписчикам (так что они могут среагировать на отмену), но
 * код, породивший событие, должен проверять {@link #cancelado()} и
 * прекращать соответствующее действие.
 *
 * <p>Реализации обязаны быть <b>thread-safe</b>: флаг обычно хранится в
 * {@code volatile}-поле или {@code AtomicBoolean}.
 */
@ApiStatus.Preview("base")
public interface LatidoCancelable {

    /** {@code true}, если событие уже было отменено одним из подписчиков. */
    boolean cancelado();

    /** Отменить событие. После этого {@link #cancelado()} возвращает {@code true}. */
    void cancelar();
}
