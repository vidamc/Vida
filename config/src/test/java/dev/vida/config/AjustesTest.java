/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AjustesTest {

    private static Ajustes sample() {
        ConfigNode.Table inner = ConfigNode.Table.builder()
                .putInt("port", 25565)
                .putString("motd", "Hello")
                .putBool("whitelist", false)
                .putDouble("ratio", 0.75)
                .build();
        ConfigNode.Table root = ConfigNode.Table.builder()
                .put("server", inner)
                .put("tags", ConfigNode.Array.builder()
                        .addString("vida").addString("demo").build())
                .build();
        return Ajustes.of(root);
    }

    @Test
    void strictGettersWork() {
        Ajustes a = sample();
        assertThat(a.getInt("server.port")).isEqualTo(25565);
        assertThat(a.getString("server.motd")).isEqualTo("Hello");
        assertThat(a.getBoolean("server.whitelist")).isFalse();
        assertThat(a.getDouble("server.ratio")).isEqualTo(0.75);
    }

    @Test
    void optionalGettersHandleMissingAndMismatch() {
        Ajustes a = sample();
        assertThat(a.findString("server.motd")).contains("Hello");
        assertThat(a.findString("server.port")).isEmpty();    // int, не строка
        assertThat(a.findInt("server.whitelist")).isEmpty();  // bool, не int
        assertThat(a.findInt("missing.value")).isEmpty();
    }

    @Test
    void defaultGettersFallBack() {
        Ajustes a = sample();
        assertThat(a.getInt("missing", 42)).isEqualTo(42);
        assertThat(a.getString("server.motd", "fallback")).isEqualTo("Hello");
        assertThat(a.getBoolean("server.nope", true)).isTrue();
    }

    @Test
    void intGetterAcceptsLongWhenFits() {
        Ajustes a = Ajustes.of(ConfigNode.Table.builder().putInt("n", 123).build());
        assertThat(a.getInt("n")).isEqualTo(123);
    }

    @Test
    void intGetterFailsOnOverflow() {
        Ajustes a = Ajustes.of(ConfigNode.Table.builder()
                .putInt("big", (long) Integer.MAX_VALUE + 1L).build());
        assertThatThrownBy(() -> a.getInt("big"))
                .isInstanceOf(AjustesAccessException.class);
    }

    @Test
    void tryGettersReturnResult() {
        Ajustes a = sample();
        assertThat(a.tryInt("server.port").unwrap()).isEqualTo(25565);
        assertThat(a.tryString("missing").unwrapErr())
                .isInstanceOf(AjustesError.Missing.class);
        assertThat(a.tryInt("server.motd").unwrapErr())
                .isInstanceOf(AjustesError.TypeMismatch.class);
    }

    @Test
    void getTableReturnsSubAjustes() {
        Ajustes a = sample();
        Ajustes sub = a.getTable("server");
        assertThat(sub.getInt("port")).isEqualTo(25565);
    }

    @Test
    void stringListIgnoresNonStringItems() {
        Ajustes a = Ajustes.of(ConfigNode.Table.builder()
                .put("xs", ConfigNode.Array.builder()
                        .addString("a")
                        .addInt(1)
                        .addString("b")
                        .build())
                .build());
        assertThat(a.findStringList("xs")).contains(java.util.List.of("a", "b"));
    }

    @Test
    void containsAndNodeExpose() {
        Ajustes a = sample();
        assertThat(a.contains("server.port")).isTrue();
        assertThat(a.contains("server.missing")).isFalse();
        assertThat(a.node("server").orElseThrow()).isInstanceOf(ConfigNode.Table.class);
    }

    @Test
    void intCanBePromotedToDouble() {
        Ajustes a = Ajustes.of(ConfigNode.Table.builder().putInt("x", 7).build());
        assertThat(a.findDouble("x")).contains(7.0);
    }
}
