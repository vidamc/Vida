/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.ajustes;

import static org.assertj.core.api.Assertions.*;

import dev.vida.config.ConfigNode;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class AjustesTipadosTest {

    private static dev.vida.config.Ajustes rawConMapa() {
        // render таблица
        ConfigNode.Table.Builder render = ConfigNode.Table.builder()
                .putInt("distance", 24L)
                .putString("quality", "alta")
                .putBool("hdr", true);
        ConfigNode.Table.Builder net = ConfigNode.Table.builder()
                .putDouble("timeout", 1.5);
        ConfigNode.Table.Builder server = ConfigNode.Table.builder()
                .putInt("ticks", 42L);

        ConfigNode.Table root = ConfigNode.Table.builder()
                .put("render", render.build())
                .put("net", net.build())
                .put("server", server.build())
                .build();
        return dev.vida.config.Ajustes.of(root);
    }

    private static final Ajuste<Integer> DISTANCIA =
            Ajuste.entero("render.distance", 32).min(2).max(64).build();

    private static final Ajuste<Integer> DISTANCIA_LIMITADA =
            Ajuste.entero("render.distance", 32).min(2).max(8).build();

    private static final Ajuste<String> CALIDAD =
            Ajuste.cadena("render.quality", "media").build();

    private static final Ajuste<Boolean> HDR =
            Ajuste.logico("render.hdr", false).build();

    private static final Ajuste<Double> TIMEOUT =
            Ajuste.flotante("net.timeout", 3.0).min(0.0).build();

    private static final Ajuste<Long> TICKS =
            Ajuste.largo("server.ticks", 20L).build();

    private static final Ajuste<Integer> AUSENTE =
            Ajuste.entero("missing.path", 7).build();

    // ----------------------------------------------------------------

    @Test
    void reads_valid_values() {
        AjustesTipados t = AjustesTipados.sobre(rawConMapa());
        assertThat(t.valor(DISTANCIA)).isEqualTo(24);
        assertThat(t.valor(CALIDAD)).isEqualTo("alta");
        assertThat(t.valor(HDR)).isTrue();
        assertThat(t.valor(TIMEOUT)).isEqualTo(1.5);
        assertThat(t.valor(TICKS)).isEqualTo(42L);
    }

    @Test
    void falls_back_to_default_when_missing() {
        AjustesTipados t = AjustesTipados.sobre(rawConMapa());
        assertThat(t.valor(AUSENTE)).isEqualTo(7);
        assertThat(t.establecido(AUSENTE)).isFalse();
        assertThat(t.leerEstricto(AUSENTE)).isEmpty();
    }

    @Test
    void uses_default_on_validation_failure() {
        AjustesTipados t = AjustesTipados.sobre(rawConMapa());
        // У raw = 24, но лимит = 8. Ожидаем fallback → default = 32.
        assertThat(t.valor(DISTANCIA_LIMITADA)).isEqualTo(32);
        assertThat(t.leerEstricto(DISTANCIA_LIMITADA)).isEmpty();
    }

    @Test
    void custom_validator_chained() {
        Ajuste<Integer> aj = Ajuste.entero("render.distance", 32)
                .min(0)
                .verificar(v -> v % 2 == 0 ? Optional.empty() : Optional.of("odd"))
                .build();
        AjustesTipados t = AjustesTipados.sobre(rawConMapa()); // 24 — чётное
        assertThat(t.valor(aj)).isEqualTo(24);
    }

    @Test
    void strict_read_returns_value_when_present() {
        AjustesTipados t = AjustesTipados.sobre(rawConMapa());
        assertThat(t.leerEstricto(DISTANCIA)).contains(24);
    }

    @Test
    void establecido_reflects_raw_contains() {
        AjustesTipados t = AjustesTipados.sobre(rawConMapa());
        assertThat(t.establecido(DISTANCIA)).isTrue();
        assertThat(t.establecido(AUSENTE)).isFalse();
    }

    @Test
    void min_max_rejects_out_of_range() {
        Ajuste<Integer> aj = Ajuste.entero("render.distance", 8).min(10).max(20).build();
        assertThat(aj.validar(5)).isNotEmpty();
        assertThat(aj.validar(25)).isNotEmpty();
        assertThat(aj.validar(15)).isEmpty();
    }
}
