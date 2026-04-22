/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.manifest;

import dev.vida.core.ApiStatus;

/**
 * Причина, по которой не удалось распарсить {@code vida.mod.json}.
 *
 * <p>Это отдельный sealed-тип (а не исключение), потому что `ManifestParser`
 * возвращает {@code Result<ModManifest, ManifestError>} — это ожидаемая
 * ветка потока управления, а не аварийная ситуация.
 */
@ApiStatus.Stable
public sealed interface ManifestError {

    /** Читаемое описание ошибки — для логов и UI лаунчера. */
    String message();

    /** Синтаксическая ошибка JSON на уровне токенов. */
    record SyntaxError(String message, int line, int column) implements ManifestError {}

    /** Обязательное поле отсутствует. */
    record MissingField(String fieldPath) implements ManifestError {
        @Override public String message() { return "missing required field '" + fieldPath + "'"; }
    }

    /** Значение имеет неправильный тип. */
    record WrongType(String fieldPath, String expected, String actual) implements ManifestError {
        @Override public String message() {
            return "field '" + fieldPath + "' expected " + expected + ", got " + actual;
        }
    }

    /** Значение нарушает ограничение (например, невалидный SemVer или id). */
    record InvalidValue(String fieldPath, String reason) implements ManifestError {
        @Override public String message() { return "field '" + fieldPath + "': " + reason; }
    }

    /** Неподдерживаемая версия схемы. */
    record UnsupportedSchema(int actual, int supportedMax) implements ManifestError {
        @Override public String message() {
            return "unsupported manifest schema " + actual + " (this loader supports up to " + supportedMax + ")";
        }
    }
}
