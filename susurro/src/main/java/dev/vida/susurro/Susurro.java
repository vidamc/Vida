/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.susurro;

import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Publico-facing facade управляемого thread-pool'а Vida.
 *
 * <h2>Характеристики пула</h2>
 * <ul>
 *   <li>количество workers по умолчанию: {@code max(2, Runtime.availableProcessors()/2)};</li>
 *   <li>именованные потоки вида {@code vida-susurro-N} (удобно в профайлере);</li>
 *   <li>daemon-потоки — не держат JVM от выхода;</li>
 *   <li>очередь приоритетная (ALTA → NORMAL → BAJA), FIFO внутри приоритета;</li>
 *   <li>back-pressure: лимиты на {@link Etiqueta} и на общий размер очереди.</li>
 * </ul>
 *
 * <h2>Использование</h2>
 * <pre>{@code
 *   Susurro sus = Susurro.iniciar(Susurro.Politica.porDefecto());
 *   HiloPrincipal hp = new HiloPrincipal();
 *
 *   Tarea<Integer> t = sus.lanzar(Prioridad.NORMAL, Etiqueta.de("mod/mining"),
 *                                 () -> calcularPesado())
 *           .enHiloPrincipal(hp, res -> System.out.println("done: " + res));
 *
 *   // где-то на тике:
 *   hp.pulso();
 *
 *   // при shutdown:
 *   sus.detener();
 * }</pre>
 */
@ApiStatus.Preview("susurro")
public final class Susurro implements AutoCloseable {

    private static final Log LOG = Log.of(Susurro.class);

    private final ThreadPoolExecutor pool;
    private final Politica politica;
    private final ConcurrentHashMap<String, AtomicInteger> activasPorEtiqueta = new ConcurrentHashMap<>();
    private final AtomicInteger pendientes = new AtomicInteger();
    private final AtomicLong rechazadas = new AtomicLong();
    private final AtomicLong completadas = new AtomicLong();
    private final AtomicLong secuencia = new AtomicLong();

    private Susurro(ThreadPoolExecutor pool, Politica politica) {
        this.pool = pool;
        this.politica = politica;
    }

    // ------------------------------------------------------------------ factory

    /** Стартовать с дефолтной политикой. */
    public static Susurro iniciar() {
        return iniciar(Politica.porDefecto());
    }

    /** Стартовать с политикой. */
    public static Susurro iniciar(Politica p) {
        Objects.requireNonNull(p, "politica");
        ThreadFactory tf = new NombradaDaemon("vida-susurro");
        // Ограниченный core=max=workers, очередь — своя приоритетная.
        @SuppressWarnings({"unchecked", "rawtypes"})
        PriorityBlockingQueue<Runnable> cola = new PriorityBlockingQueue<>(
                64, (a, b) -> ((Prioritizable) a).compareTo((Prioritizable) b));
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                p.workers(), p.workers(),
                60L, TimeUnit.SECONDS,
                cola,
                tf,
                (r, executor) -> {
                    throw new RejectedExecutionException("susurro pool отклонил задачу");
                });
        pool.allowCoreThreadTimeOut(true);
        return new Susurro(pool, p);
    }

    // ------------------------------------------------------------------ public API

    /**
     * Запустить задачу без результата.
     */
    public Tarea<Void> lanzar(Prioridad p, Etiqueta etiqueta, Runnable r) {
        Objects.requireNonNull(r, "r");
        return lanzar(p, etiqueta, () -> { r.run(); return null; });
    }

    /**
     * Запустить задачу с результатом.
     */
    public <T> Tarea<T> lanzar(Prioridad prioridad, Etiqueta etiqueta, Supplier<T> trabajo) {
        Objects.requireNonNull(prioridad, "prioridad");
        Objects.requireNonNull(etiqueta, "etiqueta");
        Objects.requireNonNull(trabajo, "trabajo");

        // Back-pressure: проверяем лимиты до submit.
        if (pendientes.get() >= politica.maxCola()) {
            rechazadas.incrementAndGet();
            CompletableFuture<T> fallido = new CompletableFuture<>();
            fallido.completeExceptionally(new RejectedExecutionException(
                    "susurro очередь переполнена (" + pendientes.get() + "/" + politica.maxCola() + ")"));
            return new Tarea<>(fallido);
        }
        AtomicInteger act = activasPorEtiqueta.computeIfAbsent(
                etiqueta.valor(), k -> new AtomicInteger());
        int limEt = politica.maxPorEtiqueta();
        if (limEt > 0 && act.get() >= limEt) {
            rechazadas.incrementAndGet();
            CompletableFuture<T> fallido = new CompletableFuture<>();
            fallido.completeExceptionally(new RejectedExecutionException(
                    "susurro лимит этикетки '" + etiqueta.valor()
                            + "' достигнут (" + act.get() + "/" + limEt + ")"));
            return new Tarea<>(fallido);
        }

        CompletableFuture<T> cf = new CompletableFuture<>();
        Tarea<T> tarea = new Tarea<>(cf);
        long seq = secuencia.incrementAndGet();
        Runnable wrapper = new SusurroRunnable<>(prioridad, seq, etiqueta, trabajo, tarea, act);
        pendientes.incrementAndGet();
        act.incrementAndGet();
        try {
            pool.execute(wrapper);
        } catch (RejectedExecutionException rex) {
            pendientes.decrementAndGet();
            act.decrementAndGet();
            rechazadas.incrementAndGet();
            cf.completeExceptionally(rex);
        }
        return tarea;
    }

    /** Глобальные счётчики наблюдаемости. */
    public Estadisticas estadisticas() {
        return new Estadisticas(
                pool.getActiveCount(),
                pendientes.get(),
                completadas.get(),
                rechazadas.get(),
                politica.workers());
    }

    /** Аккуратно остановить пул. Ожидает до 5 секунд. */
    public void detener() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                LOG.warn("susurro pool не остановился за 5 сек, выкидываю interrupt");
                pool.shutdownNow();
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() { detener(); }

    /** Политика. */
    public Politica politica() { return politica; }

    // ------------------------------------------------------------------ helpers

    private void onTerminada(AtomicInteger act) {
        pendientes.decrementAndGet();
        act.decrementAndGet();
        completadas.incrementAndGet();
    }

    // ================================================================
    //                            Politica
    // ================================================================

    /** Настройки пула. Иммутабельные. */
    public record Politica(int workers, int maxCola, int maxPorEtiqueta) {
        public Politica {
            if (workers < 1) throw new IllegalArgumentException("workers < 1");
            if (maxCola < 1) throw new IllegalArgumentException("maxCola < 1");
            if (maxPorEtiqueta < 0) throw new IllegalArgumentException("maxPorEtiqueta < 0");
        }

        public static Politica porDefecto() {
            int w = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
            return new Politica(w, 1024, 0 /* 0 = без лимита */);
        }
    }

    /** Снимок метрик. */
    public record Estadisticas(int activos, int pendientes, long completadas,
                               long rechazadas, int workers) {}

    // ================================================================
    //                       SusurroRunnable
    // ================================================================

    /** Внутренний маркер-интерфейс для приоритетной очереди. */
    private interface Prioritizable extends Comparable<Prioritizable> {
        int peso();
        long secuencia();
        @Override
        default int compareTo(Prioritizable o) {
            int c = Integer.compare(this.peso(), o.peso());
            if (c != 0) return c;
            return Long.compare(this.secuencia(), o.secuencia());
        }
    }

    private final class SusurroRunnable<T> implements Runnable, Prioritizable {
        private final Prioridad prioridad;
        private final long seq;
        private final Etiqueta etiqueta;
        private final Supplier<T> trabajo;
        private final Tarea<T> tarea;
        private final AtomicInteger act;

        SusurroRunnable(Prioridad p, long seq, Etiqueta et,
                        Supplier<T> trabajo, Tarea<T> tarea, AtomicInteger act) {
            this.prioridad = p;
            this.seq = seq;
            this.etiqueta = et;
            this.trabajo = trabajo;
            this.tarea = tarea;
            this.act = act;
        }

        @Override public int peso() { return prioridad.peso(); }
        @Override public long secuencia() { return seq; }

        @Override
        public void run() {
            tarea.marcarIniciada();
            if (tarea.revisada()) {
                onTerminada(act);
                tarea.futuro().completeExceptionally(
                        new java.util.concurrent.CancellationException("tarea cancelada antes de iniciar"));
                return;
            }
            T res;
            try {
                res = trabajo.get();
            } catch (Throwable t) {
                onTerminada(act);
                tarea.futuro().completeExceptionally(t);
                return;
            }
            onTerminada(act);
            tarea.futuro().complete(res);
        }
    }

    // ================================================================
    //                     NombradaDaemon (ThreadFactory)
    // ================================================================

    private static final class NombradaDaemon implements ThreadFactory {
        private final String prefijo;
        private final AtomicInteger cuenta = new AtomicInteger();
        NombradaDaemon(String prefijo) { this.prefijo = prefijo; }
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefijo + "-" + cuenta.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }
}
