/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base;

import static org.assertj.core.api.Assertions.*;

import dev.vida.core.Version;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ModMetadataTest {

    @Test
    void basico_minimal_ok() {
        ModMetadata m = ModMetadata.basico("demo", Version.parse("1.0.0"));
        assertThat(m.id()).isEqualTo("demo");
        assertThat(m.nombre()).isEqualTo("demo");
        assertThat(m.descripcion()).isEmpty();
        assertThat(m.autores()).isEmpty();
    }

    @Test
    void con_nombre() {
        ModMetadata m = ModMetadata.con("x", Version.parse("0.1.0"), "Extendo");
        assertThat(m.nombre()).isEqualTo("Extendo");
    }

    @Test
    void autores_defensivo_copy() {
        List<String> autoresMutables = new ArrayList<>();
        autoresMutables.add("Ana");
        ModMetadata m = new ModMetadata("x", Version.parse("1.0.0"), "X", "desc", autoresMutables);
        autoresMutables.add("Bob"); // не должно повлиять
        assertThat(m.autores()).containsExactly("Ana");
    }

    @Test
    void id_blank_prohibido() {
        assertThatThrownBy(() ->
                new ModMetadata("", Version.parse("1.0.0"), "X", "", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nombre_blank_prohibido() {
        assertThatThrownBy(() ->
                new ModMetadata("x", Version.parse("1.0.0"), "  ", "", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
