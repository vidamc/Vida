/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.culling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ValentaFrustumTest {

    @Test
    void identity_allInside() {
        ValentaFrustum f = new ValentaFrustum();
        f.update(identityMvp());

        assertThat(f.testAabb(-1, -1, -1, 1, 1, 1)).isTrue();
    }

    @Test
    void identity_farAway_stillInside() {
        ValentaFrustum f = new ValentaFrustum();
        f.update(identityMvp());

        assertThat(f.testAabb(-0.5, -0.5, -0.5, 0.5, 0.5, 0.5)).isTrue();
    }

    @Test
    void shiftedFrustum_outsideBox_culled() {
        ValentaFrustum f = new ValentaFrustum();
        double[] shifted = identityMvp();
        shifted[12] = 100.0;
        f.update(shifted);

        assertThat(f.testAabb(-1, -1, -1, 1, 1, 1)).isFalse();
    }

    @Test
    void testSection_convenience() {
        ValentaFrustum f = new ValentaFrustum();
        f.update(identityMvp());

        assertThat(f.testSection(0, 0, 0)).isTrue();
    }

    private double[] identityMvp() {
        return new double[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        };
    }
}
