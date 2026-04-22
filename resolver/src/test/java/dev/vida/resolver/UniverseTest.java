/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import static dev.vida.resolver.Providers.p;
import static dev.vida.resolver.Providers.pp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vida.core.Version;
import java.util.List;
import org.junit.jupiter.api.Test;

class UniverseTest {

    @Test
    void candidates_return_providers_sorted_by_version_desc() {
        Universe u = Universe.of(List.of(
                p("a", "1.0.0"),
                p("a", "2.0.0"),
                p("a", "1.5.0")));
        List<Provider> cs = u.candidates("a");
        assertEquals(Version.parse("2.0.0"), cs.get(0).version());
        assertEquals(Version.parse("1.5.0"), cs.get(1).version());
        assertEquals(Version.parse("1.0.0"), cs.get(2).version());
    }

    @Test
    void aliases_are_indexed() {
        Universe u = Universe.of(List.of(
                pp("forge-api", "1.0.0", List.of("fabric-api"))));
        assertEquals(1, u.candidates("forge-api").size());
        assertEquals(1, u.candidates("fabric-api").size());
        assertEquals(0, u.candidates("other").size());
    }

    @Test
    void duplicate_same_id_and_version_rejected() {
        Universe.Builder b = Universe.builder()
                .add(p("a", "1.0.0"));
        assertThrows(IllegalStateException.class, () -> b.add(p("a", "1.0.0")));
    }

    @Test
    void known_ids_include_aliases() {
        Universe u = Universe.of(List.of(
                p("a", "1.0.0"),
                pp("b", "1.0.0", List.of("b-compat"))));
        assertTrue(u.knownIds().contains("a"));
        assertTrue(u.knownIds().contains("b"));
        assertTrue(u.knownIds().contains("b-compat"));
    }

    @Test
    void size_reflects_unique_providers() {
        Universe u = Universe.of(List.of(
                p("a", "1.0.0"),
                p("a", "2.0.0"),
                p("b", "1.0.0")));
        assertEquals(3, u.size());
    }

    @Test
    void missing_query_returns_empty() {
        Universe u = Universe.of(List.of(p("a", "1.0.0")));
        assertTrue(u.candidates("nope").isEmpty());
    }
}
