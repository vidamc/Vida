/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import dev.vida.core.ApiStatus;

/**
 * Публичный интерфейс событийной шины Vida.
 *
 * <p>Шина хранит подписчиков на каждый {@link Latido} и доставляет
 * события в строго детерминированном порядке:
 * <ol>
 *   <li>сначала {@link Prioridad} (URGENTE → MONITOR);</li>
 *   <li>внутри приоритета — {@link Fase} (ANTES → PRINCIPAL → DESPUES);</li>
 *   <li>при равных приоритете и фазе — в порядке регистрации (FIFO).</li>
 * </ol>
 *
 * <p>Все методы <b>thread-safe</b>. Базовая реализация
 * {@link DefaultLatidoBus} использует copy-on-write-структуры, так что
 * {@link #emitir} не блокирует регистрацию новых подписчиков и наоборот.
 */
@ApiStatus.Stable
public interface LatidoBus {

    /**
     * Регистрирует {@link Oyente} на событие.
     *
     * @return дескриптор подписки — вызов {@link Suscripcion#cancelar()}
     *         удаляет подписчика.
     */
    <E> Suscripcion suscribir(Latido<E> tipo, Prioridad prioridad, Fase fase,
                              Ejecutor ejecutor, Oyente<? super E> oyente);

    /** Shortcut: {@link Ejecutor#SINCRONO}. */
    default <E> Suscripcion suscribir(Latido<E> tipo, Prioridad prioridad, Fase fase,
                                      Oyente<? super E> oyente) {
        return suscribir(tipo, prioridad, fase, Ejecutor.SINCRONO, oyente);
    }

    /** Shortcut: {@link Fase#PRINCIPAL} + {@link Ejecutor#SINCRONO}. */
    default <E> Suscripcion suscribir(Latido<E> tipo, Prioridad prioridad, Oyente<? super E> oyente) {
        return suscribir(tipo, prioridad, Fase.PRINCIPAL, Ejecutor.SINCRONO, oyente);
    }

    /** Shortcut: {@link Prioridad#NORMAL} + {@link Fase#PRINCIPAL} + {@link Ejecutor#SINCRONO}. */
    default <E> Suscripcion suscribir(Latido<E> tipo, Oyente<? super E> oyente) {
        return suscribir(tipo, Prioridad.NORMAL, Fase.PRINCIPAL, Ejecutor.SINCRONO, oyente);
    }

    /**
     * Доставляет событие подписчикам.
     *
     * @return агрегированный результат
     * @throws ClassCastException если {@code evento} не совместим с {@code tipo.claseEvento()}
     *                            (быстрая проверка, чтобы ошибки типа не всплывали позже)
     */
    <E> LatidoDespacho emitir(Latido<E> tipo, E evento);

    /** Количество текущих подписчиков на указанный ключ. */
    int cantidadSuscriptores(Latido<?> tipo);

    /** Полностью очищает шину (снимает все подписки). Используется в тестах и при shutdown. */
    void limpiar();

    /** Фабрика in-memory шины с дефолтными настройками. */
    static LatidoBus enMemoria() {
        return new DefaultLatidoBus();
    }
}
