/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.mods.valenta.quality.CloudRenderer;
import dev.vida.mods.valenta.quality.ParticleFilter;
import org.junit.jupiter.api.Test;

class ValentaConfigTest {

    @Test
    void defecto_hasReasonableValues() {
        ValentaConfig cfg = ValentaConfig.defecto();
        assertThat(cfg.useMultiDrawIndirect()).isTrue();
        assertThat(cfg.useCompactVertexFormat()).isTrue();
        assertThat(cfg.biomeBlendingSsbo()).isTrue();
        assertThat(cfg.blockLightSsbo()).isTrue();
        assertThat(cfg.useOcclusionQueries()).isTrue();
        assertThat(cfg.useFrustumCulling()).isTrue();
        assertThat(cfg.usePvsTree()).isTrue();
        assertThat(cfg.invalidateWhenOccluded()).isTrue();
        assertThat(cfg.particles()).isEqualTo(ParticleFilter.Mode.NINGUNO);
        assertThat(cfg.clouds()).isEqualTo(CloudRenderer.Mode.VANILLA);
        assertThat(cfg.animatedTextures()).isTrue();
        assertThat(cfg.minRenderDistanceSafe()).isEqualTo(2);
        assertThat(cfg.showGpuTimings()).isFalse();
        assertThat(cfg.showOcclusionOverlay()).isFalse();
    }

    @Test
    void meshWorkers_defaultIsPositive() {
        ValentaConfig cfg = ValentaConfig.defecto();
        assertThat(cfg.meshWorkers()).isZero();
    }
}
