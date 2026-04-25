/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.susurro;

import dev.vida.core.ApiStatus;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Обёртка над {@link CompletableFuture} с дополнительной семантикой Vida.
 *
 * <p>Отличия от «голого» CF:
 * <ul>
 *   <li>{@link #cancelar()} устанавливает {@link #estado()} в
 *       {@link Estado#CANCELADA} и пробрасывает это в workers ({@link #revisada()}
 *       можно использовать как cooperative-cancel чек);</li>
 *   <li>{@link #conPlazo(Duration)} — декоратор таймаута с корректной
 *       очисткой ресурсов;</li>
 *   <li>{@link #enHiloPrincipal(HiloPrincipal, Consumer)} — короткая запись
 *       для маршалинга успешного результата на main-thread.</li>
 * </ul>
 *
 * <p>Экземпляры создаются только через {@link Susurro}; публичного
 * конструктора нет.
 *
 * @param <T> тип результата
 */
@ApiStatus.Stable
public final class Tarea<T> {

    /** Состояние задачи. */
    public enum Estado { PENDIENTE, EN_EJECUCION, COMPLETADA, FALLADA, CANCELADA }

    private final CompletableFuture<T> futuro;
    private final AtomicReference<Estado> estado = new AtomicReference<>(Estado.PENDIENTE);

    Tarea(CompletableFuture<T> futuro) {
        this.futuro = Objects.requireNonNull(futuro, "futuro");
        // Мост состояний CF → Estado.
        // TimeoutException приравнивается к отмене — см. conPlazo().
        futuro.whenComplete((v, err) -> {
            if (err != null) {
                if (err instanceof java.util.concurrent.CancellationException
                        || err instanceof TimeoutException
                        || estado.get() == Estado.CANCELADA) {
                    estado.set(Estado.CANCELADA);
                } else {
                    estado.compareAndSet(Estado.PENDIENTE, Estado.FALLADA);
                    estado.compareAndSet(Estado.EN_EJECUCION, Estado.FALLADA);
                }
            } else {
                estado.compareAndSet(Estado.PENDIENTE, Estado.COMPLETADA);
                estado.compareAndSet(Estado.EN_EJECUCION, Estado.COMPLETADA);
            }
        });
    }

    /** Отметить задачу как начавшую исполнение. */
    void marcarIniciada() {
        estado.compareAndSet(Estado.PENDIENTE, Estado.EN_EJECUCION);
    }

    /** Текущее состояние. */
    public Estado estado() { return estado.get(); }

    /** Задача завершена любым способом. */
    public boolean terminada() {
        Estado e = estado.get();
        return e == Estado.COMPLETADA || e == Estado.FALLADA || e == Estado.CANCELADA;
    }

    /**
     * Запросить отмену. Для уже исполняющегося кода — это cooperative-сигнал;
     * worker должен периодически проверять {@link #revisada()} и завершиться.
     */
    public void cancelar() {
        estado.set(Estado.CANCELADA);
        futuro.cancel(false);
    }

    /** Worker'у: «меня отменили?». */
    public boolean revisada() {
        return estado.get() == Estado.CANCELADA || futuro.isCancelled();
    }

    /** Добавить callback на успех. */
    public Tarea<T> alCompletar(Consumer<? super T> cb) {
        Objects.requireNonNull(cb, "cb");
        futuro.thenAccept(cb);
        return this;
    }

    /** Добавить callback на ошибку. */
    public Tarea<T> alFallar(Consumer<Throwable> cb) {
        Objects.requireNonNull(cb, "cb");
        futuro.exceptionally(ex -> { cb.accept(ex); return null; });
        return this;
    }

    /** Трансформировать результат. */
    public <R> Tarea<R> mapa(Function<? super T, ? extends R> f) {
        Objects.requireNonNull(f, "f");
        return new Tarea<>(futuro.thenApply(f));
    }

    /**
     * Доставить результат на main-thread. Callback вызывается синхронно
     * при следующем {@link HiloPrincipal#pulso()}.
     */
    public Tarea<T> enHiloPrincipal(HiloPrincipal hilo, Consumer<? super T> cb) {
        Objects.requireNonNull(hilo, "hilo");
        Objects.requireNonNull(cb, "cb");
        futuro.thenAccept(v -> hilo.enviar(() -> cb.accept(v)));
        return this;
    }

    /**
     * Декорировать таймаутом. Если задача не завершилась за {@code plazo},
     * она отменяется и будущее заваливается {@link TimeoutException}.
     */
    public Tarea<T> conPlazo(Duration plazo) {
        Objects.requireNonNull(plazo, "plazo");
        if (plazo.isNegative() || plazo.isZero()) {
            throw new IllegalArgumentException("plazo <= 0: " + plazo);
        }
        // orTimeout мутирует this.futuro: по истечении он завершится
        // с TimeoutException, а наш bridge в конструкторе выставит
        // estado=CANCELADA (см. комментарий там).
        futuro.orTimeout(plazo.toMillis(), TimeUnit.MILLISECONDS);
        return this;
    }

    /**
     * Синхронно дождаться результата. Используйте <b>только</b> в тестах и
     * инструментах CLI; вызывать из main-thread Minecraft — значит заставить
     * игру фризить.
     */
    public T esperar() throws InterruptedException, ExecutionException {
        return futuro.get();
    }

    /** Синхронное ожидание с таймаутом. */
    public T esperar(Duration plazo) throws InterruptedException, ExecutionException, TimeoutException {
        return futuro.get(plazo.toMillis(), TimeUnit.MILLISECONDS);
    }

    /** Внутреннее будущее (для интеграции с пулом). */
    CompletableFuture<T> futuro() { return futuro; }
}
