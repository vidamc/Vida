/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import static org.assertj.core.api.Assertions.*;

import dev.vida.vifada.MorphSource;
import org.junit.jupiter.api.Test;

final class MorphIndexTest {

    @Test
    void empty_index_reports_zero() {
        MorphIndex idx = MorphIndex.empty();
        assertThat(idx.targetCount()).isZero();
        assertThat(idx.totalMorphs()).isZero();
        assertThat(idx.hasMorphs("x/Y")).isFalse();
        assertThat(idx.forTarget("x/Y")).isEmpty();
    }

    @Test
    void accumulates_morphs_by_target() {
        MorphSource a = new MorphSource("m/A", new byte[] {1});
        MorphSource b = new MorphSource("m/B", new byte[] {2});
        MorphSource c = new MorphSource("m/C", new byte[] {3});

        MorphIndex idx = MorphIndex.builder()
                .add("t/Target1", a)
                .add("t/Target1", b)
                .add("t/Target2", c)
                .build();

        assertThat(idx.targetCount()).isEqualTo(2);
        assertThat(idx.totalMorphs()).isEqualTo(3);
        assertThat(idx.hasMorphs("t/Target1")).isTrue();
        assertThat(idx.forTarget("t/Target1")).containsExactly(a, b);
        assertThat(idx.forTarget("t/Target2")).containsExactly(c);
        assertThat(idx.targets()).containsExactlyInAnyOrder("t/Target1", "t/Target2");
    }

    @Test
    void index_is_immutable_after_build() {
        var builder = MorphIndex.builder().add("x/Y",
                new MorphSource("m/X", new byte[] {9}));
        MorphIndex idx = builder.build();
        // Мутируем builder после build — idx не должен измениться.
        builder.add("x/Y", new MorphSource("m/X2", new byte[] {9}));
        assertThat(idx.forTarget("x/Y")).hasSize(1);
    }
}
