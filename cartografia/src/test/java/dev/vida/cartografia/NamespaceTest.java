/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NamespaceTest {

    @Test
    void constantsAreDistinct() {
        assertThat(Namespace.OBF).isNotEqualTo(Namespace.MOJMAP);
        assertThat(Namespace.MOJMAP).isNotEqualTo(Namespace.NAMED);
        assertThat(Namespace.OBF.name()).isEqualTo("obf");
        assertThat(Namespace.MOJMAP.name()).isEqualTo("mojmap");
        assertThat(Namespace.NAMED.name()).isEqualTo("named");
    }

    @Test
    void rejectsEmptyName() {
        assertThatThrownBy(() -> new Namespace(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> new Namespace(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsTooLongName() {
        String big = "x".repeat(65);
        assertThatThrownBy(() -> new Namespace(big))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringIsName() {
        assertThat(Namespace.of("intermediate").toString()).isEqualTo("intermediate");
    }

    @Test
    void compareToIsLexicographic() {
        assertThat(Namespace.of("alpha").compareTo(Namespace.of("beta"))).isLessThan(0);
        assertThat(Namespace.of("zulu").compareTo(Namespace.of("alpha"))).isGreaterThan(0);
        assertThat(Namespace.of("same").compareTo(Namespace.of("same"))).isZero();
    }
}
