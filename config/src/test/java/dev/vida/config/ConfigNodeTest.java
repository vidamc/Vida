/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConfigNodeTest {

    @Test
    void builderPreservesInsertionOrder() {
        ConfigNode.Table t = ConfigNode.Table.builder()
                .putInt("b", 2)
                .putInt("a", 1)
                .putInt("c", 3)
                .build();
        assertThat(t.entries().keySet()).containsExactly("b", "a", "c");
    }

    @Test
    void findSupportsDottedPath() {
        ConfigNode.Table inner = ConfigNode.Table.builder().putString("host", "localhost").build();
        ConfigNode.Table outer = ConfigNode.Table.builder().put("net", inner).build();
        assertThat(outer.find("net")).isInstanceOf(ConfigNode.Table.class);
        assertThat(outer.find("net.host")).isEqualTo(ConfigNode.str("localhost"));
        assertThat(outer.find("net.missing")).isNull();
        assertThat(outer.find("missing.host")).isNull();
        assertThat(outer.find("")).isSameAs(outer);
    }

    @Test
    void typeNamesAreStable() {
        assertThat(ConfigNode.str("x").typeName()).isEqualTo("string");
        assertThat(ConfigNode.integer(1).typeName()).isEqualTo("integer");
        assertThat(ConfigNode.real(1.0).typeName()).isEqualTo("float");
        assertThat(ConfigNode.bool(true).typeName()).isEqualTo("boolean");
        assertThat(ConfigNode.Table.EMPTY.typeName()).isEqualTo("table");
        assertThat(ConfigNode.Array.EMPTY.typeName()).isEqualTo("array");
    }

    @Test
    void arrayBuilderProducesImmutable() {
        ConfigNode.Array a = ConfigNode.Array.builder().addInt(1).addInt(2).addString("x").build();
        assertThat(a.size()).isEqualTo(3);
        assertThat(a.get(0)).isEqualTo(ConfigNode.integer(1));
        assertThat(a.get(2)).isEqualTo(ConfigNode.str("x"));
    }

    @Test
    void equalityByContent() {
        ConfigNode.Table a = ConfigNode.Table.builder().putInt("x", 1).build();
        ConfigNode.Table b = ConfigNode.Table.builder().putInt("x", 1).build();
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void trailingDotReturnsNull() {
        ConfigNode.Table t = ConfigNode.Table.builder().putInt("x", 1).build();
        assertThat(t.find("x.")).isNull();
        assertThat(t.find(".x")).isNull();
    }
}
