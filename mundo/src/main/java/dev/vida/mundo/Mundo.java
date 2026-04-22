/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;

/**
 * Минимальный публичный контракт мира.
 */
@ApiStatus.Preview("mundo")
public interface Mundo {

    Identifier id();

    Dimension dimension();

    Bioma biomaEn(Coordenada coordenada);

    boolean estaCargado(Coordenada coordenada);

    long tiempoDelDia();

    default boolean esDeDia() {
        long tiempo = tiempoDelDia() % 24000L;
        return tiempo >= 0L && tiempo < 12000L;
    }

    default boolean esDeNoche() {
        return !esDeDia();
    }
}
