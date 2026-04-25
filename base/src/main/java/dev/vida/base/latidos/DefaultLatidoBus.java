/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Потокобезопасная in-memory реализация {@link LatidoBus}.
 *
 * <p>Хранит по одному {@code CanalBuilder} на {@link Latido}. Чтение
 * отсортированного списка подписчиков идёт под read-lock, регистрация и
 * отмена — под write-lock. Внутри read-lock мы не вызываем пользовательский
 * код (копируем список и выходим из-под блокировки перед доставкой), так
 * что медленный подписчик не блокирует других.
 */
@ApiStatus.Stable
public final class DefaultLatidoBus implements LatidoBus {

    private static final Log LOG = Log.of(DefaultLatidoBus.class);

    private final ConcurrentHashMap<Latido<?>, Canal<?>> canales = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(); // для стабильного FIFO внутри равного приоритета/фазы

    // ================================================================

    @Override
    public <E> Suscripcion suscribir(Latido<E> tipo, Prioridad prioridad, Fase fase,
                                     Ejecutor ejecutor, Oyente<? super E> oyente) {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(prioridad, "prioridad");
        Objects.requireNonNull(fase, "fase");
        Objects.requireNonNull(ejecutor, "ejecutor");
        Objects.requireNonNull(oyente, "oyente");

        @SuppressWarnings("unchecked")
        Canal<E> canal = (Canal<E>) canales.computeIfAbsent(tipo, Canal::new);
        return canal.registrar(prioridad, fase, ejecutor, oyente, seq.incrementAndGet());
    }

    @Override
    public <E> LatidoDespacho emitir(Latido<E> tipo, E evento) {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(evento, "evento");
        if (!tipo.claseEvento().isInstance(evento)) {
            throw new ClassCastException("event " + evento.getClass().getName()
                    + " is not assignable to " + tipo.claseEvento().getName());
        }
        @SuppressWarnings("unchecked")
        Canal<E> canal = (Canal<E>) canales.get(tipo);
        if (canal == null) return LatidoDespacho.vacio();
        return canal.despachar(evento);
    }

    @Override
    public int cantidadSuscriptores(Latido<?> tipo) {
        Canal<?> canal = canales.get(tipo);
        return canal == null ? 0 : canal.tamanio();
    }

    @Override
    public void limpiar() {
        canales.clear();
    }

    // ================================================================
    //                           Canal (per event-type)
    // ================================================================

    private static final class Canal<E> {

        private final Latido<E> tipo;
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        /** Отсортированный по (peso Prioridad, peso Fase, seq) список. */
        private List<Entrada<E>> ordenado = List.of();

        Canal(Latido<E> tipo) {
            this.tipo = tipo;
        }

        Suscripcion registrar(Prioridad p, Fase f, Ejecutor ej, Oyente<? super E> o, long seq) {
            Entrada<E> entrada = new Entrada<>(p, f, ej, o, seq);
            lock.writeLock().lock();
            try {
                List<Entrada<E>> copia = new ArrayList<>(ordenado.size() + 1);
                copia.addAll(ordenado);
                // insert-sort в нужное место
                int ins = 0;
                while (ins < copia.size() && compara(copia.get(ins), entrada) <= 0) ins++;
                copia.add(ins, entrada);
                ordenado = List.copyOf(copia);
            } finally {
                lock.writeLock().unlock();
            }
            return new HandleSuscripcion<>(this, entrada);
        }

        void borrar(Entrada<E> e) {
            lock.writeLock().lock();
            try {
                List<Entrada<E>> copia = new ArrayList<>(ordenado);
                copia.remove(e);
                ordenado = List.copyOf(copia);
            } finally {
                lock.writeLock().unlock();
            }
        }

        LatidoDespacho despachar(E evento) {
            List<Entrada<E>> snapshot;
            lock.readLock().lock();
            try { snapshot = ordenado; } finally { lock.readLock().unlock(); }
            if (snapshot.isEmpty()) return LatidoDespacho.vacio();

            LatidoCancelable cancelable = evento instanceof LatidoCancelable c ? c : null;
            List<Throwable> errores = new ArrayList<>();
            int recibidos = 0;

            for (Entrada<E> en : snapshot) {
                boolean esMonitor = en.prioridad == Prioridad.MONITOR;
                if (cancelable != null && cancelable.cancelado() && !esMonitor) {
                    // Событие уже отменено → дальше только MONITOR-слушатели.
                    continue;
                }
                boolean sincrono = en.ejecutor == Ejecutor.SINCRONO;
                if (sincrono) {
                    try {
                        en.oyente.manejar(evento);
                        recibidos++;
                    } catch (Throwable t) {
                        errores.add(t);
                        LOG.warn("Latido oyente failed for " + tipo + ": " + t, t);
                    }
                } else {
                    // Асинхронно/marshalled: отменяемое событие запретим —
                    // иначе невозможно гарантировать согласованность с
                    // потоком эмиссии. MONITOR же хорошо работает асинхронно.
                    if (cancelable != null && !esMonitor) {
                        LOG.warn("Oyente с не-SINCRONO ejecutor подписан на отменяемое событие {} — "
                                + "будет запущен, но его решения об отмене игнорируются шиной", tipo);
                    }
                    try {
                        en.ejecutor.ejecutar(() -> {
                            try {
                                en.oyente.manejar(evento);
                            } catch (Throwable t) {
                                LOG.warn("Latido oyente failed for " + tipo + ": " + t, t);
                            }
                        });
                        // Считаем, что отправка задача == recibido. Реальный
                        // запуск произойдёт позднее в другом потоке.
                        recibidos++;
                    } catch (Throwable t) {
                        errores.add(t);
                        LOG.warn("Ejecutor rechazado latido oyente for {}: {}", tipo, t.toString());
                    }
                }
                if (cancelable != null && esMonitor && cancelable.cancelado()) {
                    LOG.warn("MONITOR oyente cancelled event {} — флаг игнорируется семантически",
                            tipo);
                }
            }
            return new LatidoDespacho(recibidos, errores,
                    cancelable != null && cancelable.cancelado());
        }

        int tamanio() {
            lock.readLock().lock();
            try { return ordenado.size(); } finally { lock.readLock().unlock(); }
        }

        private static int compara(Entrada<?> a, Entrada<?> b) {
            int c = Integer.compare(a.prioridad.peso(), b.prioridad.peso());
            if (c != 0) return c;
            c = Integer.compare(a.fase.peso(), b.fase.peso());
            if (c != 0) return c;
            return Long.compare(a.seq, b.seq);
        }
    }

    private record Entrada<E>(Prioridad prioridad, Fase fase, Ejecutor ejecutor,
                              Oyente<? super E> oyente, long seq) {}

    // ================================================================

    private static final class HandleSuscripcion<E> implements Suscripcion {
        private final Canal<E> canal;
        private final Entrada<E> entrada;
        private final AtomicBoolean activa = new AtomicBoolean(true);

        HandleSuscripcion(Canal<E> canal, Entrada<E> entrada) {
            this.canal = canal;
            this.entrada = entrada;
        }

        @Override public boolean activa() { return activa.get(); }

        @Override
        public void cancelar() {
            if (activa.compareAndSet(true, false)) {
                canal.borrar(entrada);
            }
        }
    }
}
