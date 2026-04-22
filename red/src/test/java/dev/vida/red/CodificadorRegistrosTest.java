/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.core.Identifier;
import org.junit.jupiter.api.Test;

class CodificadorRegistrosTest {

    @Test
    void roundtrip_para_tipos_primitivos_y_identifier() {
        CodecPaquete<PaqueteDato> codec = CodificadorRegistros.para(PaqueteDato.class);
        PaqueteDato origen = new PaqueteDato(7, 9L, false, 1.5f, 2.5d, "hola", Identifier.of("demo", "id"));

        byte[] bytes = codec.codificar(origen);
        PaqueteDato restaurado = codec.decodificar(bytes);

        assertThat(restaurado).isEqualTo(origen);
    }

    @Test
    void rechaza_campos_no_soportados() {
        assertThatThrownBy(() -> CodificadorRegistros.para(PaqueteInvalido.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no soportado");
    }

    record PaqueteDato(
            int a,
            long b,
            boolean c,
            float d,
            double e,
            String f,
            Identifier g) implements PaqueteCliente {}

    record PaqueteInvalido(java.util.List<String> lista) implements PaqueteServidor {}
}
