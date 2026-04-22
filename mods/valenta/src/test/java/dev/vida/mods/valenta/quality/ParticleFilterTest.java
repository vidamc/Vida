/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.quality;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ParticleFilterTest {

    @Test
    void ninguno_alwaysRenders() {
        ParticleFilter filter = new ParticleFilter(ParticleFilter.Mode.NINGUNO, 4);
        for (int i = 0; i < 100; i++) {
            assertThat(filter.shouldRender()).isTrue();
        }
        assertThat(filter.filteredCount()).isZero();
    }

    @Test
    void ocultar_neverRenders() {
        ParticleFilter filter = new ParticleFilter(ParticleFilter.Mode.OCULTAR, 4);
        for (int i = 0; i < 100; i++) {
            assertThat(filter.shouldRender()).isFalse();
        }
        assertThat(filter.filteredCount()).isEqualTo(100);
    }

    @Test
    void reducir_everyNth() {
        ParticleFilter filter = new ParticleFilter(ParticleFilter.Mode.REDUCIR, 4);
        int rendered = 0;
        for (int i = 0; i < 100; i++) {
            if (filter.shouldRender()) rendered++;
        }
        assertThat(rendered).isEqualTo(25);
    }

    @Test
    void modeSwitch_resetsCounter() {
        ParticleFilter filter = new ParticleFilter(ParticleFilter.Mode.REDUCIR, 4);
        filter.shouldRender();
        filter.shouldRender();
        filter.setMode(ParticleFilter.Mode.NINGUNO);
        assertThat(filter.shouldRender()).isTrue();
    }

    @Test
    void parse_modes() {
        assertThat(ParticleFilter.Mode.parse("none")).isEqualTo(ParticleFilter.Mode.NINGUNO);
        assertThat(ParticleFilter.Mode.parse("reducir")).isEqualTo(ParticleFilter.Mode.REDUCIR);
        assertThat(ParticleFilter.Mode.parse("ocultar")).isEqualTo(ParticleFilter.Mode.OCULTAR);
    }
}
