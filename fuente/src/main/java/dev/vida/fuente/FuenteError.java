/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.fuente;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;

/**
 * Ошибки прототипа data-driven загрузки.
 */
@ApiStatus.Stable
public sealed interface FuenteError {

    record ConfigInvalida(String detalle) implements FuenteError {
        public ConfigInvalida {
            Objects.requireNonNull(detalle, "detalle");
        }
    }

    record JsonInvalido(String path, String detalle) implements FuenteError {
        public JsonInvalido {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(detalle, "detalle");
        }
    }

    record CampoFaltante(String path, String campo) implements FuenteError {
        public CampoFaltante {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(campo, "campo");
        }
    }

    /** Значение не совпало с известным перечислением API ({@code bloque}/{@code objeto}). */
    record TipoDesconocido(String contexto, String valor) implements FuenteError {
        public TipoDesconocido {
            Objects.requireNonNull(contexto, "contexto");
            Objects.requireNonNull(valor, "valor");
        }
    }

    /**
     * Ссылка на вложенную loot-таблицу (см. {@code FuenteLootTable#tablasReferenciadas}) не
     * соответствует ни одному {@code .json} в {@code loot_tables/} того же контентного снимка.
     */
    record TablaLootReferenciaRota(Identifier desdeTabla, Identifier referencia) implements FuenteError {
        public TablaLootReferenciaRota {
            Objects.requireNonNull(desdeTabla, "desdeTabla");
            Objects.requireNonNull(referencia, "referencia");
        }
    }

    /**
     * В каталоге {@code worldgen} есть {@code loot_table}, указывающий на id из пространства имён модуля,
     * но соответствующего JSON в {@code loot_tables/} Fuente-снимка нет.
     */
    record WorldgenLootReferenciaRota(String archivoWorldgen, Identifier referencia) implements FuenteError {
        public WorldgenLootReferenciaRota {
            Objects.requireNonNull(archivoWorldgen, "archivoWorldgen");
            Objects.requireNonNull(referencia, "referencia");
        }
    }

    /**
     * В каталоге {@code worldgen} есть ссылка на блок модуля (block / Name+Properties / blocks), но такого
     * JSON в {@code bloques/} в снимке нет.
     */
    record WorldgenBloqueReferenciaRoto(String archivoWorldgen, Identifier referencia) implements FuenteError {
        public WorldgenBloqueReferenciaRoto {
            Objects.requireNonNull(archivoWorldgen, "archivoWorldgen");
            Objects.requireNonNull(referencia, "referencia");
        }
    }
}
