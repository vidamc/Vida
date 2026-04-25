/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.puertas;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Структурированные ошибки парсера и аппликатора пуэртас.
 */
@ApiStatus.Stable
public sealed interface PuertaError {

    /** Где именно произошла ошибка — для удобных сообщений. */
    String ubicacion();

    /** Неполный/битый заголовок {@code vida-puertas N namespace=X}. */
    record CabeceraInvalida(String ubicacion, String linea, String razon) implements PuertaError {
        public CabeceraInvalida {
            Objects.requireNonNull(ubicacion);
            Objects.requireNonNull(linea);
            Objects.requireNonNull(razon);
        }
    }

    /** Неизвестный namespace. */
    record NamespaceDesconocido(String ubicacion, String valor) implements PuertaError {
        public NamespaceDesconocido {
            Objects.requireNonNull(ubicacion);
            Objects.requireNonNull(valor);
        }
    }

    /** Неизвестная версия формата. */
    record VersionNoSoportada(String ubicacion, int version) implements PuertaError {
        public VersionNoSoportada {
            Objects.requireNonNull(ubicacion);
        }
    }

    /** Некорректная директива (неизвестное действие или цель). */
    record DirectivaInvalida(String ubicacion, int linea, String texto, String razon) implements PuertaError {
        public DirectivaInvalida {
            Objects.requireNonNull(ubicacion);
            Objects.requireNonNull(texto);
            Objects.requireNonNull(razon);
        }
    }

    /** Недостаточно токенов в строке. */
    record DirectivaTruncada(String ubicacion, int linea, String texto) implements PuertaError {
        public DirectivaTruncada {
            Objects.requireNonNull(ubicacion);
            Objects.requireNonNull(texto);
        }
    }

    /** Невалидный JVM descriptor. */
    record DescriptorMalformado(String ubicacion, int linea, String descriptor) implements PuertaError {
        public DescriptorMalformado {
            Objects.requireNonNull(ubicacion);
            Objects.requireNonNull(descriptor);
        }
    }

    /** Директива {@code mutable} применена не к полю. */
    record MutableNoAplicable(String ubicacion, int linea, String objetivo) implements PuertaError {
        public MutableNoAplicable {
            Objects.requireNonNull(ubicacion);
            Objects.requireNonNull(objetivo);
        }
    }
}
