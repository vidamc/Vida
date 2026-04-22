/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base;

import dev.vida.core.ApiStatus;

/**
 * Состояние мода в жизненном цикле Vida.
 *
 * <p>Порядок перехода:
 * {@link #CARGADO} → {@link #INICIADO} → {@link #ACTIVO} → {@link #DETENIENDOSE}
 * → {@link #DETENIDO}.
 *
 * <p>{@link #FALLO} — terminal state, куда мод может попасть из любого
 * предыдущего при неисправимой ошибке.
 */
@ApiStatus.Preview("base")
public enum EstadoMod {

    /** JAR-мод распакован, classes найдены, {@link VidaMod} ещё не вызван. */
    CARGADO,

    /** Вызван {@link VidaMod#iniciar(ModContext)}; мод успешно отработал инициализацию. */
    INICIADO,

    /** Игра запущена, мод активно получает {@code Latidos}. */
    ACTIVO,

    /** Начался процесс выключения; мод получает {@code LatidoApagado}. */
    DETENIENDOSE,

    /** Мод корректно остановлен. */
    DETENIDO,

    /** Мод упал: {@link VidaMod#iniciar} бросил исключение или зафиксирована ошибка. */
    FALLO;

    /** {@code true}, если из этого состояния запрещены любые переходы. */
    public boolean esTerminal() {
        return this == DETENIDO || this == FALLO;
    }

    /** Разрешён ли переход {@code this -> otro}. */
    public boolean puedeTransitar(EstadoMod otro) {
        if (otro == FALLO) return !esTerminal();
        return switch (this) {
            case CARGADO       -> otro == INICIADO || otro == DETENIDO;
            case INICIADO      -> otro == ACTIVO || otro == DETENIENDOSE;
            case ACTIVO        -> otro == DETENIENDOSE;
            case DETENIENDOSE  -> otro == DETENIDO;
            case DETENIDO, FALLO -> false;
        };
    }
}
