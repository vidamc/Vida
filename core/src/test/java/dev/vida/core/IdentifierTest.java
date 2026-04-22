/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IdentifierTest {

    @Test
    void constructsValid() {
        Identifier id = Identifier.of("ejemplo", "espada_sagrada");
        assertThat(id.namespace()).isEqualTo("ejemplo");
        assertThat(id.path()).isEqualTo("espada_sagrada");
        assertThat(id.toString()).isEqualTo("ejemplo:espada_sagrada");
    }

    @Test
    void allowsHierarchicalPath() {
        Identifier id = Identifier.parse("minecraft:block/stone");
        assertThat(id.namespace()).isEqualTo("minecraft");
        assertThat(id.path()).isEqualTo("block/stone");
    }

    @Test
    void parseRequiresColon() {
        assertThatThrownBy(() -> Identifier.parse("foobar"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing ':'");
    }

    @Test
    void parseRejectsMultipleColons() {
        assertThatThrownBy(() -> Identifier.parse("a:b:c"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("multiple ':'");
    }

    @ParameterizedTest
    @ValueSource(strings = { "Ejemplo:x", "ejemplo: x", "ejemplo!:x", "ejem plo:x" })
    void rejectsInvalidNamespace(String s) {
        assertThatThrownBy(() -> Identifier.parse(s))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "ejemplo:X", "ejemplo:foo bar", "ejemplo:foo$bar" })
    void rejectsInvalidPath(String s) {
        assertThatThrownBy(() -> Identifier.parse(s))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyPartsAreRejected() {
        assertThatThrownBy(() -> Identifier.of("", "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Identifier.of("x", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tryParseReturnsEmptyForInvalid() {
        assertThat(Identifier.tryParse(null)).isEmpty();
        assertThat(Identifier.tryParse("nope")).isEmpty();
        assertThat(Identifier.tryParse("ok:yes")).isEqualTo(Optional.of(Identifier.of("ok", "yes")));
    }

    @Test
    void parseWithDefaultFallsBack() {
        Identifier id = Identifier.parseWithDefault("stone", "minecraft");
        assertThat(id).isEqualTo(Identifier.of("minecraft", "stone"));

        Identifier keepsExplicit = Identifier.parseWithDefault("ejemplo:stone", "minecraft");
        assertThat(keepsExplicit.namespace()).isEqualTo("ejemplo");
    }

    @Test
    void comparableOrdersByNamespaceThenPath() {
        Identifier a = Identifier.of("aaa", "z");
        Identifier b = Identifier.of("bbb", "a");
        Identifier a2 = Identifier.of("aaa", "zz");
        assertThat(a).isLessThan(b);
        assertThat(a).isLessThan(a2);
    }

    @Test
    void rejectsOverlyLongIdentifier() {
        String longPath = "x".repeat(300);
        assertThatThrownBy(() -> Identifier.of("ns", longPath))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too long");
    }

    @Test
    void isValidReportsCorrectly() {
        assertThat(Identifier.isValid("ok:yes")).isTrue();
        assertThat(Identifier.isValid("bad")).isFalse();
        assertThat(Identifier.isValid(null)).isFalse();
    }
}
