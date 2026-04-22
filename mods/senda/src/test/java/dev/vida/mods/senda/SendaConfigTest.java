/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.senda;

import static org.assertj.core.api.Assertions.*;

import dev.vida.base.ajustes.AjustesTipados;
import dev.vida.base.ajustes.DefaultAjustesTipados;
import dev.vida.config.Ajustes;
import dev.vida.config.ConfigNode;
import org.junit.jupiter.api.Test;

final class SendaConfigTest {

    // ---------------------------------------------------------------- defecto

    @Test
    void defecto_valores() {
        SendaConfig cfg = SendaConfig.defecto();
        assertThat(cfg.maxPuntosPorDimension()).isEqualTo(50);
        assertThat(cfg.dimensionInicial()).isEqualTo("overworld");
    }

    // ---------------------------------------------------------------- desde(AjustesTipados)

    @Test
    void desde_customValues() {
        AjustesTipados tipados = build(ConfigNode.Table.builder()
                .putInt("maxPuntosPorDimension", 100)
                .putString("dimensionInicial", "nether")
                .build());

        SendaConfig cfg = SendaConfig.desde(tipados);

        assertThat(cfg.maxPuntosPorDimension()).isEqualTo(100);
        assertThat(cfg.dimensionInicial()).isEqualTo("nether");
    }

    @Test
    void desde_emptyConfig_usesDefaults() {
        AjustesTipados tipados = build(ConfigNode.Table.EMPTY);

        SendaConfig cfg = SendaConfig.desde(tipados);

        assertThat(cfg.maxPuntosPorDimension()).isEqualTo(50);
        assertThat(cfg.dimensionInicial()).isEqualTo("overworld");
    }

    @Test
    void desde_maxPuntosBelowMin_fallsToDefault() {
        // Validador min(1) rejects 0 → default 50
        AjustesTipados tipados = build(ConfigNode.Table.builder()
                .putInt("maxPuntosPorDimension", 0)
                .build());

        SendaConfig cfg = SendaConfig.desde(tipados);

        assertThat(cfg.maxPuntosPorDimension()).isEqualTo(50);
    }

    @Test
    void desde_maxPuntosAboveMax_fallsToDefault() {
        AjustesTipados tipados = build(ConfigNode.Table.builder()
                .putInt("maxPuntosPorDimension", 9999)
                .build());

        SendaConfig cfg = SendaConfig.desde(tipados);

        assertThat(cfg.maxPuntosPorDimension()).isEqualTo(50);
    }

    @Test
    void desde_invalidDimension_fallsToDefault() {
        AjustesTipados tipados = build(ConfigNode.Table.builder()
                .putString("dimensionInicial", "invalid")
                .build());

        SendaConfig cfg = SendaConfig.desde(tipados);

        assertThat(cfg.dimensionInicial()).isEqualTo("overworld");
    }

    @Test
    void desde_endDimension() {
        AjustesTipados tipados = build(ConfigNode.Table.builder()
                .putString("dimensionInicial", "end")
                .build());

        assertThat(SendaConfig.desde(tipados).dimensionInicial()).isEqualTo("end");
    }

    @Test
    void desde_dimensionCaseInsensitive() {
        AjustesTipados tipados = build(ConfigNode.Table.builder()
                .putString("dimensionInicial", "NETHER")
                .build());

        // Валидатор сравнивает lowercase, и normalization также lowercase
        assertThat(SendaConfig.desde(tipados).dimensionInicial()).isEqualTo("nether");
    }

    // ---------------------------------------------------------------- helpers

    private static AjustesTipados build(ConfigNode.Table table) {
        return new DefaultAjustesTipados(Ajustes.of(table));
    }
}
