/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery;

import dev.vida.core.ApiStatus;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Источник мода: дисковый файл или встроенный (вложенный) архив.
 *
 * <p>Содержит достаточно информации, чтобы повторно открыть архив
 * ({@link #open()}) и построить диагностическое имя ({@link #id()}).
 */
@ApiStatus.Stable
public sealed interface ModSource {

    /** Стабильный идентификатор источника для логов/ошибок/кэша. */
    String id();

    /**
     * Открывает ридер архива. Вызывающий обязан закрыть его.
     *
     * @throws IOException при I/O-ошибке
     */
    ZipReader open() throws IOException;

    /**
     * Размер архива в байтах. Для {@link OnDisk} берётся из файловой системы
     * (возвращается {@code -1}, если недоступен). Для {@link Embedded} —
     * длина содержимого.
     */
    long sizeBytes();

    // ------------------------------------------------------------ OnDisk

    /** Файл на диске. */
    record OnDisk(Path path) implements ModSource {
        public OnDisk {
            Objects.requireNonNull(path, "path");
        }

        @Override
        public String id() {
            return path.toString();
        }

        @Override
        public ZipReader open() throws IOException {
            return new FileZipReader(path);
        }

        @Override
        public long sizeBytes() {
            try {
                return java.nio.file.Files.size(path);
            } catch (IOException ignore) {
                return -1;
            }
        }
    }

    // ---------------------------------------------------------- Embedded

    /** Вложенный архив: хранит байты и ссылку на родителя. */
    record Embedded(ModSource parent, String innerPath, byte[] bytes) implements ModSource {
        public Embedded {
            Objects.requireNonNull(parent, "parent");
            Objects.requireNonNull(innerPath, "innerPath");
            Objects.requireNonNull(bytes, "bytes");
            if (innerPath.isBlank()) {
                throw new IllegalArgumentException("innerPath is blank");
            }
        }

        @Override
        public String id() {
            return parent.id() + "!/" + innerPath;
        }

        @Override
        public ZipReader open() throws IOException {
            return new BytesZipReader(id(), bytes);
        }

        @Override
        public long sizeBytes() {
            return bytes.length;
        }
    }
}
