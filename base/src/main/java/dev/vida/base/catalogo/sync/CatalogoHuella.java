/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.catalogo.sync;

import dev.vida.core.ApiStatus;

/**
 * Huella estable del contenido de un {@link dev.vida.base.catalogo.Catalogo}
 * para negociación cliente/servidor (fase preparatoria).
 *
 * @param esquemaVersion versión del algoritmo de huella (incrementar si cambia el cómputo)
 * @param digestHex      SHA-256 hex del snapshot serializado del catálogo
 */
@ApiStatus.Preview("catalogo-sync")
public record CatalogoHuella(int esquemaVersion, String digestHex) {}
