/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.fuente;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Ошибки прототипа data-driven загрузки.
 */
@ApiStatus.Preview("loader")
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
}
