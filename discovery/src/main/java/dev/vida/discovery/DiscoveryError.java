/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery;

import dev.vida.core.ApiStatus;
import dev.vida.manifest.ManifestError;

/**
 * Sealed-иерархия ошибок, возвращаемых {@link ModScanner} и связанными
 * компонентами. Все варианты — record-ы с {@link #message()} для логов.
 */
@ApiStatus.Stable
public sealed interface DiscoveryError {

    /** Короткое описание проблемы для логов/отчёта. */
    String message();

    /** Путь / идентификатор источника, с которым связана ошибка. */
    String source();

    // ------------------------------------------------------------ variants

    /** Путь не существует или не является директорией. */
    record NotADirectory(String source) implements DiscoveryError {
        @Override public String message() {
            return "mods path is not a directory: " + source;
        }
    }

    /** Файл не открывается как zip/jar. */
    record BadArchive(String source, String detail) implements DiscoveryError {
        @Override public String message() {
            return "bad archive '" + source + "': " + detail;
        }
    }

    /** В архиве отсутствует {@code vida.mod.json}. */
    record ManifestMissing(String source) implements DiscoveryError {
        @Override public String message() {
            return "no vida.mod.json in " + source;
        }
    }

    /** Манифест не распарсился. */
    record ManifestParse(String source, ManifestError cause) implements DiscoveryError {
        @Override public String message() {
            return "bad manifest in " + source + ": " + cause.message();
        }
    }

    /** Вложенный JAR, объявленный в манифесте, не найден или повреждён. */
    record NestedMissing(String source, String innerPath, String detail) implements DiscoveryError {
        @Override public String message() {
            return "nested jar '" + innerPath + "' in " + source + ": " + detail;
        }
    }

    /** Глубина вложенности JAR превысила лимит скана. */
    record NestingTooDeep(String source, int depth, int limit) implements DiscoveryError {
        @Override public String message() {
            return "nested jar too deep at " + source + ": " + depth + " > " + limit;
        }
    }

    /** Произвольная I/O ошибка. */
    record IoError(String source, String detail) implements DiscoveryError {
        @Override public String message() {
            return "I/O error at " + source + ": " + detail;
        }
    }
}
