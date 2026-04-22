/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.core.Identifier;
import org.junit.jupiter.api.Test;

class MundoTiposTest {

    @Test
    void mundo_default_methods_detectan_dia_y_noche() {
        Mundo dia = new MundoFalso(1000L);
        Mundo noche = new MundoFalso(13000L);

        assertThat(dia.esDeDia()).isTrue();
        assertThat(dia.esDeNoche()).isFalse();
        assertThat(noche.esDeDia()).isFalse();
        assertThat(noche.esDeNoche()).isTrue();
    }

    @Test
    void bioma_valida_humedad_y_helpers() {
        Bioma nevado = new Bioma(
                Identifier.of("minecraft", "snowy_plains"),
                0.0f,
                0.6f,
                Bioma.Precipitacion.NIEVE);

        assertThat(nevado.esFrio()).isTrue();
        assertThat(nevado.tienePrecipitacion()).isTrue();

        assertThatThrownBy(() -> new Bioma(
                Identifier.of("minecraft", "desierto"),
                2.0f,
                1.5f,
                Bioma.Precipitacion.NINGUNA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("humedad");
    }

    @Test
    void dimensiones_built_in_son_consistentes() {
        assertThat(Dimension.OVERWORLD.natural()).isTrue();
        assertThat(Dimension.NETHER.techoFijo()).isTrue();
        assertThat(Dimension.END.permiteCama()).isFalse();
    }

    private record MundoFalso(long tiempoDelDia) implements Mundo {
        @Override
        public Identifier id() {
            return Identifier.of("vida", "test");
        }

        @Override
        public Dimension dimension() {
            return Dimension.OVERWORLD;
        }

        @Override
        public Bioma biomaEn(Coordenada coordenada) {
            return new Bioma(
                    Identifier.of("minecraft", "plains"),
                    0.8f,
                    0.4f,
                    Bioma.Precipitacion.LLUVIA);
        }

        @Override
        public boolean estaCargado(Coordenada coordenada) {
            return true;
        }
    }
}
