/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.quality;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RenderDistanceManagerTest {

    @Test
    void initial_effective_equalsTarget() {
        var mgr = new RenderDistanceManager(2, 12);
        assertThat(mgr.effective()).isEqualTo(12);
        assertThat(mgr.isTransitioning()).isFalse();
    }

    @Test
    void setTarget_beginsTransition() {
        var mgr = new RenderDistanceManager(2, 12);
        mgr.setTarget(8);
        assertThat(mgr.isTransitioning()).isTrue();
    }

    @Test
    void tick_gradualllyApproachesTarget() {
        var mgr = new RenderDistanceManager(2, 12);
        mgr.setTarget(10);

        for (int i = 0; i < 4; i++) mgr.tick();
        assertThat(mgr.effective()).isEqualTo(11);

        for (int i = 0; i < 4; i++) mgr.tick();
        assertThat(mgr.effective()).isEqualTo(10);
        assertThat(mgr.isTransitioning()).isFalse();
    }

    @Test
    void minSafe_enforced() {
        var mgr = new RenderDistanceManager(4, 8);
        mgr.setTarget(1);
        assertThat(mgr.target()).isEqualTo(4);
    }

    @Test
    void increasing_distance() {
        var mgr = new RenderDistanceManager(2, 8);
        mgr.setTarget(10);

        for (int i = 0; i < 8; i++) mgr.tick();
        assertThat(mgr.effective()).isEqualTo(10);
    }
}
