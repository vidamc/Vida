/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vida.core.Version;
import dev.vida.core.VersionRange;
import dev.vida.manifest.ModDependencies;
import dev.vida.manifest.ModManifest;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ManifestAdapterTest {

    @Test
    void converts_required_optional_and_incompat_dependencies() {
        ModDependencies deps = new ModDependencies(
                Map.of("core", VersionRange.parse(">=1.0.0")),
                Map.of("shader", VersionRange.parse("^2.0.0")),
                Map.of("legacy-mod", VersionRange.parse("*")));
        ModManifest m = ModManifest.builder("demo", Version.of(1, 2, 3), "Demo")
                .dependencies(deps)
                .build();

        Provider p = ManifestAdapter.toProvider(m);
        assertEquals("demo", p.id());
        assertEquals(Version.of(1, 2, 3), p.version());
        assertEquals(3, p.dependencies().size());

        long required = p.dependencies().stream()
                .filter(d -> d.kind() == DependencyKind.REQUIRED).count();
        long optional = p.dependencies().stream()
                .filter(d -> d.kind() == DependencyKind.OPTIONAL).count();
        long incompat = p.dependencies().stream()
                .filter(d -> d.kind() == DependencyKind.INCOMPATIBLE).count();
        assertEquals(1, required);
        assertEquals(1, optional);
        assertEquals(1, incompat);
    }

    @Test
    void no_deps_yields_empty_provider() {
        ModManifest m = ModManifest.builder("demo", Version.of(1, 0, 0), "Demo").build();
        Provider p = ManifestAdapter.toProvider(m);
        assertTrue(p.dependencies().isEmpty());
        assertTrue(p.provides().isEmpty());
    }

    @Test
    void attachment_is_passed_through() {
        ModManifest m = ModManifest.builder("demo", Version.of(1, 0, 0), "Demo").build();
        Object payload = new Object();
        Provider p = ManifestAdapter.toProvider(m, payload);
        assertNotNull(p.attachment());
        assertSame(payload, p.attachment());
    }
}
