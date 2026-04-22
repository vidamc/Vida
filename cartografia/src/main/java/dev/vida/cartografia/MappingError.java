/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia;

import dev.vida.core.ApiStatus;

/**
 * Структурированные ошибки, возвращаемые чтением/разбором мэппингов.
 *
 * <p>Все реализации — record-ы с человекочитаемым {@link #message()}. Это
 * sealed-иерархия: матчинг по {@code instanceof} ограничен фиксированным
 * набором потомков.
 */
@ApiStatus.Stable
public sealed interface MappingError {

    /** Короткое описание, пригодное для логов. */
    String message();

    /** Синтаксическая ошибка во входном потоке (Proguard/CTG и т.п.). */
    record SyntaxError(String source, int line, String detail) implements MappingError {
        @Override public String message() {
            return source + ":" + line + ": " + detail;
        }
    }

    /** Неподдерживаемая версия файла мэппингов (например, .ctg v2 на v1-читателе). */
    record UnsupportedVersion(String source, int major, int minor) implements MappingError {
        @Override public String message() {
            return source + ": unsupported mapping file version " + major + "." + minor;
        }
    }

    /** Файл повреждён: битая сигнатура, неполные записи, несогласованные длины. */
    record Corrupted(String source, String detail) implements MappingError {
        @Override public String message() {
            return source + ": corrupted mapping file — " + detail;
        }
    }

    /** Конфликт: дубликат класса/члена в одном namespace. */
    record DuplicateEntry(String path, String namespace) implements MappingError {
        @Override public String message() {
            return "duplicate mapping entry '" + path + "' in namespace '" + namespace + "'";
        }
    }

    /** Низкоуровневая I/O-ошибка. */
    record IoError(String source, String detail) implements MappingError {
        @Override public String message() {
            return source + ": I/O error — " + detail;
        }
    }
}
