/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.core.ApiStatus;

/**
 * Punto de delegación reservado para morfos que no pueden enlazar clases del mod
 * directamente (cargador de juego vs {@link ModLoader}). Los mods cliente pueden
 * registrar aquí callbacks cargados desde su propio JAR.
 */
@ApiStatus.Internal
public final class ModClassDispatch {

    private ModClassDispatch() {}
}
