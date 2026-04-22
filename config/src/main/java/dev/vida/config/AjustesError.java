/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.config;

import dev.vida.core.ApiStatus;

/**
 * Ошибки чтения и валидации конфигурации Vida.
 *
 * <p>Возвращаются из {@link AjustesLoader} в {@code Result} и из типизированных
 * геттеров {@link Ajustes} при явных конфликтах; все подтипы несут читаемое
 * {@link #message()} для логов и UI.
 */
@ApiStatus.Stable
public sealed interface AjustesError {

    String message();

    /** Ошибка синтаксиса TOML. Координаты — 1-based. */
    record SyntaxError(String source, int line, int column, String detail) implements AjustesError {
        @Override public String message() {
            return "syntax error in " + source + " at " + line + ":" + column + " — " + detail;
        }
    }

    /** Отсутствует обязательное значение и нет дефолта. */
    record Missing(String path) implements AjustesError {
        @Override public String message() { return "missing required config value '" + path + "'"; }
    }

    /** Значение имеет несовместимый тип. */
    record TypeMismatch(String path, String expected, String actual) implements AjustesError {
        @Override public String message() {
            return "config '" + path + "' expected " + expected + ", got " + actual;
        }
    }

    /** Значение вне допустимого диапазона. */
    record OutOfRange(String path, String reason) implements AjustesError {
        @Override public String message() {
            return "config '" + path + "' out of range: " + reason;
        }
    }

    /** Имя профиля задано, но соответствующая секция отсутствует. */
    record UnknownProfile(String profile) implements AjustesError {
        @Override public String message() { return "unknown profile '" + profile + "'"; }
    }
}
