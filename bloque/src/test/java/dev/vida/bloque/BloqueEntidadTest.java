/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.core.Identifier;
import org.junit.jupiter.api.Test;

class BloqueEntidadTest {

    private static final class CofreCtx implements ContextoBloqueEntidad {
        int cantidadOro = 0;

        @Override
        public byte[] serializar() {
            return new byte[] { (byte) cantidadOro };
        }

        @Override
        public void deserializar(byte[] datos) {
            this.cantidadOro = datos.length > 0 ? datos[0] & 0xFF : 0;
        }
    }

    @Test
    void fabrica_produce_nuevos_contextos() {
        BloqueEntidad<CofreCtx> be = new BloqueEntidad<>(
                Identifier.of("ejemplo", "cofre"),
                PropiedadesBloque.con(MaterialBloque.MADERA).construir(),
                CofreCtx::new);

        CofreCtx a = be.crearContexto();
        CofreCtx b = be.crearContexto();
        assertThat(a).isNotSameAs(b);
        a.cantidadOro = 42;
        assertThat(b.cantidadOro).isZero();
    }

    @Test
    void fabrica_null_lanza() {
        BloqueEntidad<CofreCtx> be = new BloqueEntidad<>(
                Identifier.of("ejemplo", "cofre"),
                PropiedadesBloque.con(MaterialBloque.MADERA).construir(),
                () -> null);
        assertThatThrownBy(be::crearContexto)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void serializacion_round_trip() {
        CofreCtx ctx = new CofreCtx();
        ctx.cantidadOro = 17;
        byte[] out = ctx.serializar();

        CofreCtx ctx2 = new CofreCtx();
        ctx2.deserializar(out);
        assertThat(ctx2.cantidadOro).isEqualTo(17);
    }
}
