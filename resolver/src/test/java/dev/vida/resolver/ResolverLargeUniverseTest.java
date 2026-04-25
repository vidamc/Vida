/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.core.Result;
import dev.vida.core.Version;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** Регрессия «много модов»: детерминированный резолв без взрыва по времени (CI). */
final class ResolverLargeUniverseTest {

    @Test
    @Timeout(120)
    void resolve_three_hundred_twenty_independent_mods() {
        int n = 320;
        List<Provider> providers = IntStream.range(0, n)
                .mapToObj(i -> Provider.builder("m" + i, Version.parse("1.0.0")).build())
                .toList();
        Universe.Builder ub = Universe.builder();
        Set<String> roots = new LinkedHashSet<>();
        for (Provider p : providers) {
            ub.add(p);
            roots.add(p.id());
        }
        long t0 = System.nanoTime();
        Result<Resolution, ResolverError> r =
                Resolver.resolve(roots, ub.build(), ResolverOptions.DEFAULTS);
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        assertThat(r.isOk()).isTrue();
        assertThat(r.unwrap().selected()).hasSize(n);
        assertThat(ms).isLessThan(60_000L);
    }
}
