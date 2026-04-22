/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.susurro;

import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * «Главный поток» — очередь Runnable'ов, исполняемых на тике игры.
 *
 * <p>API намеренно простое: {@link #enviar(Runnable)} кладёт задачу,
 * {@link #pulso()} — сливает очередь, исполняет последовательно все
 * отложенные callbacks. Вызов {@code pulso()} ожидается из главного тика
 * Minecraft (в Vida это делает адаптер {@code vida-loader}), но модуль
 * side-agnostic: можно вызывать из тестов напрямую.
 *
 * <p>Потокобезопасность: {@link #enviar} можно звать из любого потока;
 * {@link #pulso} должен вызываться строго из одного (main) потока.
 */
@ApiStatus.Preview("susurro")
public final class HiloPrincipal {

    private static final Log LOG = Log.of(HiloPrincipal.class);

    private final ConcurrentLinkedQueue<Runnable> cola = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pendientes = new AtomicInteger(0);
    private final int limitePorPulso;
    /** Поток, из которого было первое успешное {@link #pulso()}. {@code -1} до первого вызова. */
    private volatile long idHilo = -1;

    public HiloPrincipal() {
        this(Integer.MAX_VALUE);
    }

    /**
     * @param limitePorPulso максимум задач, которые обрабатываются за один
     *                       {@code pulso()}. Предохранитель от перегрузки
     *                       тика при внезапном flush большого количества
     *                       завершённых worker-задач.
     */
    public HiloPrincipal(int limitePorPulso) {
        if (limitePorPulso < 1) {
            throw new IllegalArgumentException("limitePorPulso < 1: " + limitePorPulso);
        }
        this.limitePorPulso = limitePorPulso;
    }

    /**
     * Отправить задачу на main-thread.
     *
     * @throws NullPointerException если {@code r == null}
     */
    public void enviar(Runnable r) {
        Objects.requireNonNull(r, "r");
        cola.add(r);
        pendientes.incrementAndGet();
    }

    /**
     * Шаг процессинга. Должен вызываться из одного и того же потока
     * (в терминах Minecraft — из главного тика). Возвращает количество
     * реально выполненных задач.
     */
    public int pulso() {
        long actual = Thread.currentThread().threadId();
        long esperado = idHilo;
        if (esperado == -1) {
            idHilo = actual;
        } else if (esperado != actual) {
            throw new IllegalStateException(
                    "pulso() вызван из потока " + actual
                            + ", но главный поток — " + esperado);
        }

        int hechas = 0;
        while (hechas < limitePorPulso) {
            Runnable r = cola.poll();
            if (r == null) break;
            pendientes.decrementAndGet();
            try {
                r.run();
            } catch (Throwable t) {
                LOG.warn("HiloPrincipal callback выкинул {}: {}", t.getClass().getSimpleName(), t.getMessage(), t);
            }
            hechas++;
        }
        return hechas;
    }

    /** Сколько задач ожидают pulso'а. */
    public int pendientes() { return pendientes.get(); }

    /** Отвязывает main-thread id (для тестов/reset). */
    public void reiniciar() {
        cola.clear();
        pendientes.set(0);
        idHilo = -1;
    }
}
