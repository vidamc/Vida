/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta;

import dev.vida.core.ApiStatus;

/**
 * Instancia viva del mod (pantalla de vídeo y otros puntos de integración
 * que no reciben {@link dev.vida.base.ModContext}).
 */
@ApiStatus.Internal
public final class ValentaRuntime {

    private static volatile ValentaMod instancia;

    private ValentaRuntime() {}

    public static void enlazar(ValentaMod mod) {
        instancia = mod;
    }

    public static void limpiar() {
        instancia = null;
    }

    public static ValentaMod instancia() {
        return instancia;
    }
}
