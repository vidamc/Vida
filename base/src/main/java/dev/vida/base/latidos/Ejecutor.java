/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import dev.vida.core.ApiStatus;
import dev.vida.susurro.Etiqueta;
import dev.vida.susurro.HiloPrincipal;
import dev.vida.susurro.Prioridad;
import dev.vida.susurro.Susurro;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Стратегия исполнения обработчика события — часть «Latidos profundos».
 *
 * <p>Исторически все {@link Oyente}-ы {@link LatidoBus} выполнялись
 * синхронно в потоке {@link LatidoBus#emitir}. Это быстро, но опасно для
 * тяжёлых задач (например, сериализации чанка или сетевого I/O), которые
 * не должны блокировать игровой tick. {@code Ejecutor} даёт явный контроль
 * над тем, в каком потоке будет вызван слушатель.
 *
 * <p>Реализация — SAM-интерфейс: можно передать лямбду
 * {@code r -> pool.execute(r)} для своего пула. Все фабрики ниже возвращают
 * потокобезопасные, иммутабельные экземпляры.
 *
 * <h2>Контракт</h2>
 * <ul>
 *   <li>исполнение задачи должно произойти «когда-нибудь», но строго
 *       один раз;</li>
 *   <li>ошибки внутри {@code ejecutar} не должны пробрасываться в шину —
 *       их логирует {@link DefaultLatidoBus};</li>
 *   <li>порядок следования задач в рамках одного {@code Ejecutor}-а —
 *       как он сам его соблюдает. {@link #SINCRONO} — строго по порядку,
 *       {@link #susurro(Susurro, Prioridad, Etiqueta)} — по приоритету.</li>
 * </ul>
 */
@ApiStatus.Stable
@FunctionalInterface
public interface Ejecutor {

    /** Немедленное синхронное исполнение в вызывающем потоке. */
    Ejecutor SINCRONO = new SincronoEjecutor();

    /**
     * Запланировать запуск обработчика.
     *
     * @param r действие, которое {@link DefaultLatidoBus} уже обернул в
     *          {@code try/catch}.
     */
    void ejecutar(Runnable r);

    // ================================================================
    //                           ФАБРИКИ
    // ================================================================

    /** Исполнять задачу в следующем «пульсе» главного потока. */
    static Ejecutor hiloPrincipal(HiloPrincipal hp) {
        Objects.requireNonNull(hp, "hiloPrincipal");
        return hp::enviar;
    }

    /** Отправлять задачу в {@link Susurro}-пул с заданным приоритетом и этикеткой. */
    static Ejecutor susurro(Susurro s, Prioridad prioridad, Etiqueta etiqueta) {
        Objects.requireNonNull(s, "susurro");
        Objects.requireNonNull(prioridad, "prioridad");
        Objects.requireNonNull(etiqueta, "etiqueta");
        return r -> s.lanzar(prioridad, etiqueta, r);
    }

    /** Удобная фабрика {@link #susurro(Susurro, Prioridad, Etiqueta)} с базовыми дефолтами. */
    static Ejecutor susurro(Susurro s) {
        return susurro(s, Prioridad.NORMAL, Etiqueta.de("vida/latido"));
    }

    /** Однопоточный executor для тестов и строгого сериализованного порядка. */
    static Ejecutor serializado(String nombre) {
        Objects.requireNonNull(nombre, "nombre");
        return new SerializadoEjecutor(nombre);
    }

    // ================================================================

    /** Реализация синхронного executor'а (идентична {@code r.run()}). */
    final class SincronoEjecutor implements Ejecutor {
        SincronoEjecutor() {}
        @Override public void ejecutar(Runnable r) { r.run(); }
        @Override public String toString() { return "Ejecutor.SINCRONO"; }
    }

    /**
     * Единственный worker-тред + FIFO. Никаких lock'ов на hot-path: каждое
     * действие — создание {@code Thread} сильно дороже задачи, поэтому
     * используется общий поток под {@link Object}-монитором.
     */
    final class SerializadoEjecutor implements Ejecutor {
        private final Object lock = new Object();
        private final java.util.ArrayDeque<Runnable> cola = new java.util.ArrayDeque<>();
        private final String nombre;
        private final AtomicLong seq = new AtomicLong();
        private Thread hilo;

        SerializadoEjecutor(String nombre) {
            this.nombre = nombre;
        }

        @Override
        public void ejecutar(Runnable r) {
            Objects.requireNonNull(r, "r");
            synchronized (lock) {
                cola.addLast(r);
                if (hilo == null) {
                    hilo = new Thread(this::loop,
                            "vida-ejecutor-" + nombre + "-" + seq.incrementAndGet());
                    hilo.setDaemon(true);
                    hilo.start();
                }
                lock.notifyAll();
            }
        }

        private void loop() {
            while (true) {
                Runnable next;
                synchronized (lock) {
                    while (cola.isEmpty()) {
                        try {
                            lock.wait(1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            hilo = null;
                            return;
                        }
                        if (cola.isEmpty()) {
                            hilo = null;
                            return;
                        }
                    }
                    next = cola.pollFirst();
                }
                try {
                    next.run();
                } catch (Throwable t) {
                    // Ejecutor не должен пробрасывать — шина этим займётся.
                }
            }
        }

        @Override
        public String toString() { return "Ejecutor.serializado(" + nombre + ")"; }
    }
}
