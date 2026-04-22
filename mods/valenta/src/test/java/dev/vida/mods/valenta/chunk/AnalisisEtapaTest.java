/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.chunk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class AnalisisEtapaTest {

    @Test
    void emptyDirty_returnsEmpty() {
        var analisis = new AnalisisEtapa(64);
        List<AnalisisEtapa.SectionRequest> result = analisis.analizar(
                List.of(), 0, 64, 0, 16);
        assertThat(result).isEmpty();
    }

    @Test
    void closeSections_higherPriority() {
        var analisis = new AnalisisEtapa(64);
        List<int[]> dirty = List.of(
                new int[]{0, 4, 0},
                new int[]{10, 4, 10}
        );
        List<AnalisisEtapa.SectionRequest> result = analisis.analizar(
                dirty, 8, 68, 8, 16);
        assertThat(result).hasSizeGreaterThanOrEqualTo(1);
        assertThat(result.get(0).sectionX()).isEqualTo(0);
    }

    @Test
    void farSections_filteredOut() {
        var analisis = new AnalisisEtapa(64);
        List<int[]> dirty = List.of(new int[]{100, 4, 100});
        List<AnalisisEtapa.SectionRequest> result = analisis.analizar(
                dirty, 0, 64, 0, 4);
        assertThat(result).isEmpty();
    }

    @Test
    void cap_respectedWhenTooMany() {
        var analisis = new AnalisisEtapa(3);
        List<int[]> dirty = List.of(
                new int[]{0, 4, 0}, new int[]{1, 4, 0},
                new int[]{0, 4, 1}, new int[]{1, 4, 1},
                new int[]{2, 4, 0}
        );
        List<AnalisisEtapa.SectionRequest> result = analisis.analizar(
                dirty, 8, 68, 8, 16);
        assertThat(result).hasSize(3);
    }

    @Test
    void sortedByPriorityThenDistance() {
        var analisis = new AnalisisEtapa(64);
        List<int[]> dirty = List.of(
                new int[]{8, 4, 8},
                new int[]{0, 4, 0}
        );
        List<AnalisisEtapa.SectionRequest> result = analisis.analizar(
                dirty, 8, 68, 8, 16);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).distanceSq()).isLessThanOrEqualTo(result.get(1).distanceSq());
    }

    @Test
    void maxRequestsPerFrame_validation() {
        assertThatThrownBy(() -> new AnalisisEtapa(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
