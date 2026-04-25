/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.puertas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PuertasIndiceTest {

    @Test
    void indice_agrupa_varias_directivas_mismo_clase() {
        PuertaDirectiva a = new PuertaDirectiva(
                Accion.ACCESIBLE,
                Objetivo.CLASE,
                "net/a/X",
                Optional.empty(),
                Optional.empty(),
                2);
        PuertaDirectiva b = new PuertaDirectiva(
                Accion.MUTABLE,
                Objetivo.CAMPO,
                "net/a/X",
                Optional.of("f"),
                Optional.of("I"),
                3);
        Map<String, List<PuertaDirectiva>> ix = AplicadorPuertas.indicePorClase(List.of(a, b));
        assertThat(ix.get("net/a/X")).containsExactly(a, b);
    }
}
