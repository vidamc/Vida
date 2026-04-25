/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.ajustes;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class AjustesSincronizacionCatalogoTest {

    @AfterEach
    void cleanup() {
        AjustesSincronizacionCatalogo.resetForTests();
    }

    @Test
    void sincronizar_registers_ruta() {
        Ajuste<Integer> a = Ajuste.entero("world.tick", 20).sincronizar().build();
        assertThat(a.sincronizarConServidor()).isTrue();
        assertThat(AjustesSincronizacionCatalogo.rutasRegistradas()).containsExactly("world.tick");
        assertThat(AjustesSincronizacionCatalogo.filtrar(Map.of("world.tick", "7", "other", "x")))
                .containsExactly(Map.entry("world.tick", "7"));
    }
}
