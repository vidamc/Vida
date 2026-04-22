/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.core;

import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Единый фасад логирования Vida поверх SLF4J.
 *
 * <p>Все модули проекта обязаны логировать через этот класс, а не через
 * {@code System.out}/{@code System.err} или прямое обращение к SLF4J — это
 * даёт:
 *
 * <ul>
 *   <li>единый именнной неймспейс логгеров ({@code vida.&lt;ownerClass&gt;});</li>
 *   <li>возможность позже подменить бэкенд без изменения call-sites;</li>
 *   <li>ленивые {@code Supplier}-перегрузки для дорогих сообщений.</li>
 * </ul>
 *
 * <p>Тип иммутабелен и thread-safe: создаётся один раз на тип и кэшируется.
 */
@ApiStatus.Stable
public final class Log {

    private final Logger slf4j;

    private Log(Logger slf4j) {
        this.slf4j = slf4j;
    }

    /** Логгер для заданного класса. Имя логгера: {@code vida.<fqcn>}. */
    public static Log of(Class<?> owner) {
        Objects.requireNonNull(owner, "owner");
        return new Log(LoggerFactory.getLogger("vida." + owner.getName()));
    }

    /** Логгер с произвольным именем. */
    public static Log of(String name) {
        Objects.requireNonNull(name, "name");
        return new Log(LoggerFactory.getLogger(name));
    }

    // ------------------------------------------------------------------ TRACE
    public boolean isTraceEnabled() { return slf4j.isTraceEnabled(); }
    public void trace(String msg) { slf4j.trace(msg); }
    public void trace(String fmt, Object... args) { slf4j.trace(fmt, args); }
    public void trace(Supplier<String> msg) {
        if (slf4j.isTraceEnabled()) slf4j.trace(msg.get());
    }

    // ------------------------------------------------------------------ DEBUG
    public boolean isDebugEnabled() { return slf4j.isDebugEnabled(); }
    public void debug(String msg) { slf4j.debug(msg); }
    public void debug(String fmt, Object... args) { slf4j.debug(fmt, args); }
    public void debug(Supplier<String> msg) {
        if (slf4j.isDebugEnabled()) slf4j.debug(msg.get());
    }

    // ------------------------------------------------------------------ INFO
    public boolean isInfoEnabled() { return slf4j.isInfoEnabled(); }
    public void info(String msg) { slf4j.info(msg); }
    public void info(String fmt, Object... args) { slf4j.info(fmt, args); }
    public void info(Supplier<String> msg) {
        if (slf4j.isInfoEnabled()) slf4j.info(msg.get());
    }

    // ------------------------------------------------------------------ WARN
    public void warn(String msg) { slf4j.warn(msg); }
    public void warn(String fmt, Object... args) { slf4j.warn(fmt, args); }
    public void warn(String msg, Throwable t) { slf4j.warn(msg, t); }

    // ------------------------------------------------------------------ ERROR
    public void error(String msg) { slf4j.error(msg); }
    public void error(String fmt, Object... args) { slf4j.error(fmt, args); }
    public void error(String msg, Throwable t) { slf4j.error(msg, t); }

    /**
     * Возвращает внутренний SLF4J-логгер. Использовать только в интеграционных
     * местах, где это нужно объективно (например, прокидывание контекста MDC).
     */
    @ApiStatus.Internal
    public Logger delegate() {
        return slf4j;
    }

    @Override
    public String toString() {
        return "Log[" + slf4j.getName() + "]";
    }
}
