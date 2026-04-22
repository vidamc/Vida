/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.culling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PvsTreeTest {

    @Test
    void empty_alwaysPotentiallyVisible() {
        PvsTree tree = PvsTree.empty();
        assertThat(tree.isPotentiallyVisible(0, 0, 0, 5, 5, 5)).isTrue();
    }

    @Test
    void empty_entryCountIsZero() {
        assertThat(PvsTree.empty().entryCount()).isZero();
    }

    @Test
    void lookup_missingSection_returnsNull() {
        PvsTree tree = PvsTree.empty();
        assertThat(tree.lookup(0, 0, 0)).isNull();
    }

    @Test
    void packKey_differentCoords_differentKeys() {
        long k1 = PvsTree.packKey(0, 0, 0);
        long k2 = PvsTree.packKey(1, 0, 0);
        long k3 = PvsTree.packKey(0, 1, 0);
        long k4 = PvsTree.packKey(0, 0, 1);
        assertThat(k1).isNotEqualTo(k2);
        assertThat(k1).isNotEqualTo(k3);
        assertThat(k1).isNotEqualTo(k4);
    }

    @Test
    void packKey_sameCoords_sameKey() {
        assertThat(PvsTree.packKey(42, 7, -10))
                .isEqualTo(PvsTree.packKey(42, 7, -10));
    }

    @Test
    void pvsEntry_isVisible_forIncludedKeys() {
        long[] keys = {
                PvsTree.packKey(0, 0, 0),
                PvsTree.packKey(1, 0, 0),
                PvsTree.packKey(0, 1, 0)
        };
        java.util.Arrays.sort(keys);
        PvsTree.PvsEntry entry = new PvsTree.PvsEntry(PvsTree.packKey(0, 0, 0), keys);

        assertThat(entry.isVisible(PvsTree.packKey(0, 0, 0))).isTrue();
        assertThat(entry.isVisible(PvsTree.packKey(1, 0, 0))).isTrue();
        assertThat(entry.isVisible(PvsTree.packKey(5, 5, 5))).isFalse();
    }
}
