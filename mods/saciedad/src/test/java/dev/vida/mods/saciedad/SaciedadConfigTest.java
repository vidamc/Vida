/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.saciedad;

import static org.assertj.core.api.Assertions.*;

import dev.vida.base.ajustes.AjustesTipados;
import dev.vida.base.ajustes.DefaultAjustesTipados;
import dev.vida.config.Ajustes;
import dev.vida.config.ConfigNode;
import org.junit.jupiter.api.Test;

final class SaciedadConfigTest {

    // ---------------------------------------------------------------- parseColor

    @Test
    void parseColor_argb8() {
        assertThat(SaciedadConfig.parseColor("FFFFA500")).isEqualTo((int) 0xFFFFA500L);
    }

    @Test
    void parseColor_rgb6_addsFullAlpha() {
        assertThat(SaciedadConfig.parseColor("FFA500")).isEqualTo((int) 0xFFFFA500L);
    }

    @Test
    void parseColor_withHash() {
        assertThat(SaciedadConfig.parseColor("#FF0000")).isEqualTo((int) 0xFFFF0000L);
    }

    @Test
    void parseColor_lowercase() {
        assertThat(SaciedadConfig.parseColor("ff0000")).isEqualTo((int) 0xFFFF0000L);
    }

    @Test
    void parseColor_invalid_fallsBackToAmber() {
        assertThat(SaciedadConfig.parseColor("ZZZZZZZZ")).isEqualTo((int) 0xFFFFA500L);
    }

    @Test
    void parseColor_empty_fallsBackToAmber() {
        assertThat(SaciedadConfig.parseColor("")).isEqualTo((int) 0xFFFFA500L);
    }

    // ---------------------------------------------------------------- Posicion.desde

    @Test
    void posicion_arriba() {
        assertThat(SaciedadConfig.Posicion.desde("arriba")).isEqualTo(SaciedadConfig.Posicion.ARRIBA);
    }

    @Test
    void posicion_encima() {
        assertThat(SaciedadConfig.Posicion.desde("ENCIMA")).isEqualTo(SaciedadConfig.Posicion.ENCIMA);
    }

    @Test
    void posicion_abajo_default() {
        assertThat(SaciedadConfig.Posicion.desde("abajo")).isEqualTo(SaciedadConfig.Posicion.ABAJO);
        assertThat(SaciedadConfig.Posicion.desde("unknown")).isEqualTo(SaciedadConfig.Posicion.ABAJO);
    }

    @Test
    void posicion_trim_and_case() {
        assertThat(SaciedadConfig.Posicion.desde("  ARRIBA  ")).isEqualTo(SaciedadConfig.Posicion.ARRIBA);
    }

    // ---------------------------------------------------------------- defecto

    @Test
    void defecto_valores() {
        SaciedadConfig cfg = SaciedadConfig.defecto();
        assertThat(cfg.color()).isEqualTo((int) 0xFFFFA500L);
        assertThat(cfg.mostrarSiempre()).isFalse();
        assertThat(cfg.posicion()).isEqualTo(SaciedadConfig.Posicion.ABAJO);
    }

    // ---------------------------------------------------------------- desde(AjustesTipados)

    @Test
    void desde_allCustomValues() {
        AjustesTipados tipados = buildTipados(ConfigNode.Table.builder()
                .putString("color", "FF0000FF")
                .putBool("mostrarSiempre", true)
                .putString("posicion", "arriba")
                .build());

        SaciedadConfig cfg = SaciedadConfig.desde(tipados);

        assertThat(cfg.color()).isEqualTo((int) 0xFF0000FFL);
        assertThat(cfg.mostrarSiempre()).isTrue();
        assertThat(cfg.posicion()).isEqualTo(SaciedadConfig.Posicion.ARRIBA);
    }

    @Test
    void desde_emptyConfig_usesDefaults() {
        AjustesTipados tipados = buildTipados(ConfigNode.Table.EMPTY);

        SaciedadConfig cfg = SaciedadConfig.desde(tipados);

        assertThat(cfg.color()).isEqualTo((int) 0xFFFFA500L);
        assertThat(cfg.mostrarSiempre()).isFalse();
        assertThat(cfg.posicion()).isEqualTo(SaciedadConfig.Posicion.ABAJO);
    }

    @Test
    void desde_invalidColor_fallsToDefault() {
        AjustesTipados tipados = buildTipados(ConfigNode.Table.builder()
                .putString("color", "NOT_A_HEX")
                .build());

        SaciedadConfig cfg = SaciedadConfig.desde(tipados);

        assertThat(cfg.color()).isEqualTo((int) 0xFFFFA500L);
    }

    @Test
    void desde_encinaPosicion() {
        AjustesTipados tipados = buildTipados(ConfigNode.Table.builder()
                .putString("posicion", "encima")
                .build());

        assertThat(SaciedadConfig.desde(tipados).posicion())
                .isEqualTo(SaciedadConfig.Posicion.ENCIMA);
    }

    @Test
    void desde_mostrarSiempreTrue() {
        AjustesTipados tipados = buildTipados(ConfigNode.Table.builder()
                .putBool("mostrarSiempre", true)
                .build());

        assertThat(SaciedadConfig.desde(tipados).mostrarSiempre()).isTrue();
    }

    // ---------------------------------------------------------------- helpers

    private static AjustesTipados buildTipados(ConfigNode.Table table) {
        return new DefaultAjustesTipados(Ajustes.of(table));
    }
}
