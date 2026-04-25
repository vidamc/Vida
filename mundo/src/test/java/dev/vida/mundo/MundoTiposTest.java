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
    void dimension_limites_predeterminados_alineados_con_vanilla121() {
        assertThat(Dimension.OVERWORLD.limitesVerticalesPredeterminados())
                .isEqualTo(LimitesVerticales.overworldVanilla121());
        assertThat(Dimension.NETHER.limitesVerticalesPredeterminados())
                .isEqualTo(LimitesVerticales.netherVanilla121());
        assertThat(Dimension.END.limitesVerticalesPredeterminados())
                .isEqualTo(LimitesVerticales.endVanilla121());
    }

    @Test
    void mundo_default_altura_y_limites_explicitos() {
        assertThat(new MundoFalso(0L).enRangoDeAltura(new Coordenada(0, 319, 0))).isTrue();
        assertThat(new MundoFalso(0L).enRangoDeAltura(new Coordenada(0, 320, 0))).isFalse();

        Mundo custom = new MundoEstatico(
                Identifier.of("vida", "test"),
                Dimension.OVERWORLD,
                0L,
                new Bioma(
                        Identifier.of("minecraft", "plains"),
                        0.5f,
                        0.5f,
                        Bioma.Precipitacion.NINGUNA),
                false,
                LimitesVerticales.de(0, 10));
        assertThat(custom.limitesVerticales().maxY()).isEqualTo(10);
        assertThat(custom.enRangoDeAltura(new Coordenada(0, 10, 0))).isTrue();
        assertThat(custom.enRangoDeAltura(new Coordenada(0, 11, 0))).isFalse();
    }

    @Test
    void mundo_estatico_respeta_bioma_y_carga() {
        Bioma nevado = new Bioma(
                Identifier.of("minecraft", "snowy_plains"),
                0.0f,
                0.6f,
                Bioma.Precipitacion.NIEVE);
        Mundo est = new MundoEstatico(
                Identifier.of("vida", "test_dim"),
                Dimension.OVERWORLD,
                6000L,
                nevado,
                true);

        assertThat(est.biomaEn(new Coordenada(0, 70, 0))).isEqualTo(nevado);
        assertThat(est.estaCargado(new Coordenada(0, 70, 0))).isTrue();
    }

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
    void mundo_estatico_puede_fijar_bloque_registrado() {
        Mundo m = new MundoEstatico(
                Identifier.of("vida", "t"),
                Dimension.OVERWORLD,
                0L,
                new Bioma(Identifier.of("minecraft", "plains"), 0.5f, 0.5f, Bioma.Precipitacion.NINGUNA),
                true,
                null,
                Identifier.of("minecraft", "bed"));
        assertThat(m.bloqueRegistradoEn(new Coordenada(0, 64, 0)))
                .contains(Identifier.of("minecraft", "bed"));
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
