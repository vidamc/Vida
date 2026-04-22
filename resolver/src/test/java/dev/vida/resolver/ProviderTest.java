/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vida.core.Version;
import dev.vida.core.VersionRange;
import org.junit.jupiter.api.Test;

class ProviderTest {

    @Test
    void builder_rejects_blank_id() {
        assertThrows(IllegalArgumentException.class,
                () -> Provider.builder("", Version.of(1, 0, 0)));
    }

    @Test
    void builder_rejects_blank_alias() {
        Provider.Builder b = Provider.builder("a", Version.of(1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> b.provides(""));
    }

    @Test
    void builder_ignores_self_alias() {
        Provider p = Provider.builder("a", Version.of(1, 0, 0))
                .provides("a")
                .build();
        assertTrue(p.provides().isEmpty(), "self-alias must be dropped");
    }

    @Test
    void offers_matches_id_and_aliases() {
        Provider p = Provider.builder("a", Version.of(1, 0, 0))
                .provides("legacy-a")
                .provides("compat")
                .build();
        assertTrue(p.offers("a"));
        assertTrue(p.offers("legacy-a"));
        assertTrue(p.offers("compat"));
        assertFalse(p.offers("other"));
    }

    @Test
    void equality_is_keyed_on_id_plus_version() {
        Provider p1 = Provider.builder("a", Version.of(1, 0, 0)).build();
        Provider p2 = Provider.builder("a", Version.of(1, 0, 0))
                .dependency(Dependency.required("b", VersionRange.ANY))
                .build();
        // Равны, даже если зависимости отличаются: универс держит не более одного p(id,ver).
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void attachment_roundtrips() {
        Object payload = new Object();
        Provider p = Provider.builder("a", Version.of(1, 0, 0))
                .attachment(payload)
                .build();
        assertNotNull(p.attachment());
        assertEquals(payload, p.attachment());
    }
}
