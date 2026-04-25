/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TelemetriaV1Test {

    @BeforeEach
    void resetState() {
        TelemetriaV1.limpiarParaPruebas();
        System.clearProperty("vida.telemetry.enabled");
    }

    @AfterEach
    void tearDown() {
        TelemetriaV1.limpiarParaPruebas();
        System.clearProperty("vida.telemetry.enabled");
    }

    @Test
    void snapshot_has_only_aggregate_keys_when_enabled() {
        System.setProperty("vida.telemetry.enabled", "true");
        TelemetriaV1.registrarArranqueFrioNanos(12_500_000L);
        TelemetriaV1.cuentaTickLibre();
        TelemetriaV1.agregaNanosTransformMorph(999L);

        var snap = TelemetriaV1.snapshotSinPii();
        assertThat(snap.keySet()).containsExactlyInAnyOrder(
                "coldStartNanos", "freezeEvents", "transformMorphNanosTotal");
        assertThat(snap.get("coldStartNanos")).isEqualTo(12_500_000L);
        assertThat(snap.get("freezeEvents")).isEqualTo(1L);
        assertThat(snap.get("transformMorphNanosTotal")).isEqualTo(999L);
        for (String k : snap.keySet()) {
            assertThat(k.toLowerCase()).doesNotContain("user", "host", "email");
        }
    }

    @Test
    void disabled_skips_writes() {
        assertThat(TelemetriaV1.habilitada()).isFalse();
        TelemetriaV1.registrarArranqueFrioNanos(1L);
        var snap = TelemetriaV1.snapshotSinPii();
        assertThat(snap).doesNotContainKey("coldStartNanos");
        assertThat(snap.get("freezeEvents")).isEqualTo(0L);
    }
}
