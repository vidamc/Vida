/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.core;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Контейнер результата: либо успех ({@code Ok<T>}), либо ошибка ({@code Err<E>}).
 *
 * <p>Используется как более выразительная альтернатива исключениям в тех местах,
 * где ошибка ожидаемая часть потока управления (разбор манифестов, резолв
 * зависимостей, чтение конфигов).
 *
 * @param <T> тип значения при успехе
 * @param <E> тип ошибки
 */
@ApiStatus.Stable
public sealed interface Result<T, E> permits Result.Ok, Result.Err {

    /** Успешное значение. */
    static <T, E> Result<T, E> ok(T value) {
        return new Ok<>(value);
    }

    /** Ошибка. */
    static <T, E> Result<T, E> err(E error) {
        return new Err<>(error);
    }

    /** Оборачивает возможно-null значение как {@code Ok/Err(missing)}. */
    static <T, E> Result<T, E> ofNullable(T value, Supplier<E> onMissing) {
        return value == null ? err(onMissing.get()) : ok(value);
    }

    /**
     * Запускает {@code supplier} и превращает выброшенное исключение в {@link Err}
     * с помощью {@code onThrow}.
     */
    static <T, E, X extends Throwable> Result<T, E> catching(
            ThrowingSupplier<T, X> supplier, Function<? super X, ? extends E> onThrow) {
        try {
            return ok(supplier.get());
        } catch (Throwable t) {
            @SuppressWarnings("unchecked")
            X x = (X) t;
            return err(onThrow.apply(x));
        }
    }

    // ---------------------------------------------------------- query

    boolean isOk();
    default boolean isErr() { return !isOk(); }

    /**
     * Возвращает значение или кидает {@link NoSuchElementException}, если это ошибка.
     */
    T unwrap();

    /**
     * Возвращает ошибку или кидает {@link NoSuchElementException}, если это успех.
     */
    E unwrapErr();

    /** Значение при успехе, иначе {@code fallback}. */
    T orElse(T fallback);

    /** Значение при успехе, иначе — результат {@code fn} от ошибки. */
    T orElseGet(Function<? super E, ? extends T> fn);

    /** Значение при успехе, иначе бросает исключение, сконструированное из ошибки. */
    <X extends Throwable> T orElseThrow(Function<? super E, ? extends X> fn) throws X;

    // ---------------------------------------------------------- transform

    <U> Result<U, E> map(Function<? super T, ? extends U> fn);

    <F> Result<T, F> mapErr(Function<? super E, ? extends F> fn);

    <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> fn);

    Result<T, E> peek(Consumer<? super T> onOk);

    Result<T, E> peekErr(Consumer<? super E> onErr);

    // ---------------------------------------------------------- bridge

    Optional<T> ok();
    Optional<E> err();

    // ================================================================
    //                          implementations
    // ================================================================

    record Ok<T, E>(T value) implements Result<T, E> {
        public Ok { /* null разрешён: иногда полезно; проверяем явно */ }

        @Override public boolean isOk() { return true; }
        @Override public T unwrap() { return value; }
        @Override public E unwrapErr() { throw new NoSuchElementException("Result.Ok has no error"); }
        @Override public T orElse(T fallback) { return value; }
        @Override public T orElseGet(Function<? super E, ? extends T> fn) { return value; }
        @Override public <X extends Throwable> T orElseThrow(Function<? super E, ? extends X> fn) { return value; }

        @Override public <U> Result<U, E> map(Function<? super T, ? extends U> fn) {
            return new Ok<>(fn.apply(value));
        }
        @Override public <F> Result<T, F> mapErr(Function<? super E, ? extends F> fn) {
            return new Ok<>(value);
        }
        @Override public <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> fn) {
            return Objects.requireNonNull(fn.apply(value), "flatMap returned null Result");
        }
        @Override public Result<T, E> peek(Consumer<? super T> onOk) { onOk.accept(value); return this; }
        @Override public Result<T, E> peekErr(Consumer<? super E> onErr) { return this; }
        @Override public Optional<T> ok()  { return Optional.ofNullable(value); }
        @Override public Optional<E> err() { return Optional.empty(); }
    }

    record Err<T, E>(E error) implements Result<T, E> {
        public Err { Objects.requireNonNull(error, "error"); }

        @Override public boolean isOk() { return false; }
        @Override public T unwrap() {
            throw new NoSuchElementException("Result.Err has no value: " + error);
        }
        @Override public E unwrapErr() { return error; }
        @Override public T orElse(T fallback) { return fallback; }
        @Override public T orElseGet(Function<? super E, ? extends T> fn) { return fn.apply(error); }
        @Override public <X extends Throwable> T orElseThrow(Function<? super E, ? extends X> fn) throws X {
            throw fn.apply(error);
        }

        @Override public <U> Result<U, E> map(Function<? super T, ? extends U> fn) { return new Err<>(error); }
        @Override public <F> Result<T, F> mapErr(Function<? super E, ? extends F> fn) {
            return new Err<>(fn.apply(error));
        }
        @Override public <U> Result<U, E> flatMap(Function<? super T, ? extends Result<U, E>> fn) {
            return new Err<>(error);
        }
        @Override public Result<T, E> peek(Consumer<? super T> onOk) { return this; }
        @Override public Result<T, E> peekErr(Consumer<? super E> onErr) { onErr.accept(error); return this; }
        @Override public Optional<T> ok()  { return Optional.empty(); }
        @Override public Optional<E> err() { return Optional.of(error); }
    }

    /** Функциональный интерфейс для {@link Result#catching}. */
    @FunctionalInterface
    interface ThrowingSupplier<T, X extends Throwable> {
        T get() throws X;
    }
}
