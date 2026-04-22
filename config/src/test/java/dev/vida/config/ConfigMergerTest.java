/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConfigMergerTest {

    @Test
    void overlayAddsNewKeys() {
        ConfigNode.Table base = ConfigNode.Table.builder().putInt("a", 1).build();
        ConfigNode.Table ov   = ConfigNode.Table.builder().putInt("b", 2).build();
        ConfigNode.Table out  = ConfigMerger.merge(base, ov);
        assertThat(out.get("a")).isEqualTo(ConfigNode.integer(1));
        assertThat(out.get("b")).isEqualTo(ConfigNode.integer(2));
    }

    @Test
    void overlayOverridesSameKey() {
        ConfigNode.Table base = ConfigNode.Table.builder().putInt("a", 1).build();
        ConfigNode.Table ov   = ConfigNode.Table.builder().putInt("a", 9).build();
        ConfigNode.Table out  = ConfigMerger.merge(base, ov);
        assertThat(out.get("a")).isEqualTo(ConfigNode.integer(9));
    }

    @Test
    void tablesAreMergedDeeply() {
        ConfigNode.Table baseInner = ConfigNode.Table.builder()
                .putInt("port", 25565).putString("motd", "base").build();
        ConfigNode.Table ovInner   = ConfigNode.Table.builder()
                .putString("motd", "overlay").build();

        ConfigNode.Table base = ConfigNode.Table.builder().put("srv", baseInner).build();
        ConfigNode.Table ov   = ConfigNode.Table.builder().put("srv", ovInner).build();

        ConfigNode.Table merged = ConfigMerger.merge(base, ov);
        ConfigNode.Table sub = (ConfigNode.Table) merged.get("srv");
        // Порт из base сохранился, motd заменён
        assertThat(sub.get("port")).isEqualTo(ConfigNode.integer(25565));
        assertThat(sub.get("motd")).isEqualTo(ConfigNode.str("overlay"));
    }

    @Test
    void arraysAreReplacedNotMerged() {
        ConfigNode.Table base = ConfigNode.Table.builder().put("list",
                ConfigNode.Array.builder().addInt(1).addInt(2).build()).build();
        ConfigNode.Table ov   = ConfigNode.Table.builder().put("list",
                ConfigNode.Array.builder().addInt(9).build()).build();
        ConfigNode.Table out = ConfigMerger.merge(base, ov);
        ConfigNode.Array a = (ConfigNode.Array) out.get("list");
        assertThat(a.size()).isEqualTo(1);
        assertThat(a.get(0)).isEqualTo(ConfigNode.integer(9));
    }

    @Test
    void typeMismatchOverlayWins() {
        ConfigNode.Table base = ConfigNode.Table.builder().putInt("x", 1).build();
        ConfigNode.Table ov   = ConfigNode.Table.builder().putString("x", "auto").build();
        ConfigNode.Table out  = ConfigMerger.merge(base, ov);
        assertThat(out.get("x")).isEqualTo(ConfigNode.str("auto"));
    }

    @Test
    void emptyOverlayReturnsBase() {
        ConfigNode.Table base = ConfigNode.Table.builder().putInt("a", 1).build();
        ConfigNode.Table out = ConfigMerger.merge(base, ConfigNode.Table.EMPTY);
        assertThat(out).isSameAs(base);
    }

    @Test
    void emptyBaseReturnsOverlay() {
        ConfigNode.Table ov = ConfigNode.Table.builder().putInt("a", 1).build();
        ConfigNode.Table out = ConfigMerger.merge(ConfigNode.Table.EMPTY, ov);
        assertThat(out).isSameAs(ov);
    }

    @Test
    void layerAppliesOverlaysInOrder() {
        ConfigNode.Table base = ConfigNode.Table.builder().putInt("a", 1).putInt("b", 2).build();
        ConfigNode.Table l1   = ConfigNode.Table.builder().putInt("b", 20).putInt("c", 30).build();
        ConfigNode.Table l2   = ConfigNode.Table.builder().putInt("c", 300).build();
        ConfigNode.Table out  = ConfigMerger.layer(base, l1, l2);
        assertThat(out.get("a")).isEqualTo(ConfigNode.integer(1));
        assertThat(out.get("b")).isEqualTo(ConfigNode.integer(20));
        assertThat(out.get("c")).isEqualTo(ConfigNode.integer(300));
    }
}
