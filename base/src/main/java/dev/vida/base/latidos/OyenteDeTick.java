/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import dev.vida.core.ApiStatus;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Shortcut-аннотация для подписки на {@link dev.vida.base.latidos.eventos.LatidoPulso}.
 *
 * <p>В отличие от голого {@link EjecutorLatido}, аннотация добавляет
 * частотный throttling по корневому серверному тику: {@code tps=20}
 * означает «каждый тик», {@code tps=1} — примерно раз в секунду.
 */
@ApiStatus.Preview("base")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OyenteDeTick {

    /** Желаемая частота вызова в диапазоне {@code [1..20]}. */
    int tps() default 20;

    /** Тип исполнителя. */
    EjecutorLatido.Kind kind() default EjecutorLatido.Kind.SINCRONO;

    /** Этикетка для {@link EjecutorLatido.Kind#SUSURRO}. */
    String etiqueta() default "vida/tick";

    /** Приоритет очереди Susurro. */
    EjecutorLatido.Prioridad prioridad() default EjecutorLatido.Prioridad.NORMAL;

    /** Приоритет подписки в шине. */
    EjecutorLatido.PrioridadBus prioridadBus() default EjecutorLatido.PrioridadBus.NORMAL;

    /** Фаза подписки в шине. */
    EjecutorLatido.FaseBus fase() default EjecutorLatido.FaseBus.PRINCIPAL;
}
