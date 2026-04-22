/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import static dev.vida.resolver.Providers.p;
import static dev.vida.resolver.Providers.req;
import static dev.vida.resolver.Providers.uni;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.vida.core.Result;
import dev.vida.core.Version;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResolverBasicTest {

    @Test
    void empty_roots_succeeds_with_no_selection() {
        Universe u = uni(p("a", "1.0.0"));
        Result<Resolution, ResolverError> r = Resolver.resolve(Set.of(), u);
        assertTrue(r.isOk());
        assertEquals(0, r.unwrap().selected().size());
        assertEquals(0, r.unwrap().decisions());
    }

    @Test
    void single_root_no_deps_is_picked() {
        Universe u = uni(p("a", "1.0.0"));
        Resolution res = Resolver.resolve(Set.of("a"), u).unwrap();
        assertEquals(1, res.selected().size());
        assertEquals(Version.parse("1.0.0"), res.selected().get("a").version());
    }

    @Test
    void newest_is_picked_by_default() {
        Universe u = uni(
                p("a", "1.0.0"),
                p("a", "2.3.1"),
                p("a", "2.0.0"));
        Resolution res = Resolver.resolve(Set.of("a"), u).unwrap();
        assertEquals(Version.parse("2.3.1"), res.selected().get("a").version());
    }

    @Test
    void transitive_required_dep_pulled_in() {
        Universe u = uni(
                p("root", "1.0.0", req("lib", ">=1.0.0")),
                p("lib", "1.2.0"),
                p("lib", "2.0.0"));
        Resolution res = Resolver.resolve(Set.of("root"), u).unwrap();
        assertEquals(2, res.selected().size());
        assertEquals(Version.parse("2.0.0"), res.selected().get("lib").version());
    }

    @Test
    void oldest_strategy_picks_lowest() {
        Universe u = uni(
                p("a", "1.0.0"),
                p("a", "1.5.0"),
                p("a", "2.0.0"));
        Resolution res = Resolver.resolve(Set.of("a"), u,
                ResolverOptions.DEFAULTS.withStrategy(ResolverStrategy.OLDEST)).unwrap();
        assertEquals(Version.parse("1.0.0"), res.selected().get("a").version());
    }

    @Test
    void stable_first_prefers_release_over_prerelease_even_if_newer() {
        Universe u = uni(
                p("a", "1.0.0"),
                p("a", "2.0.0-alpha"));
        // С NEWEST pre-release сам по себе не пройдёт без seed'а в range, поэтому
        // явно ослабляем: range "*" не пустит pre-release. Для чистоты теста — STABLE_FIRST.
        Resolution res = Resolver.resolve(Set.of("a"), u,
                ResolverOptions.DEFAULTS.withStrategy(ResolverStrategy.STABLE_FIRST)).unwrap();
        assertEquals(Version.parse("1.0.0"), res.selected().get("a").version(),
                "STABLE_FIRST should bucket stable candidates ahead of pre-releases");
    }

    @Test
    void null_or_blank_root_rejected() {
        Universe u = uni(p("a", "1.0.0"));
        assertThrows(IllegalArgumentException.class,
                () -> Resolver.resolve(Set.of("  "), u));
    }

    @Test
    void failure_returns_Err_with_non_null_error() {
        Universe u = uni(p("root", "1.0.0", req("lib", ">=1.0.0")));
        Result<Resolution, ResolverError> r = Resolver.resolve(Set.of("root"), u);
        assertTrue(r.isErr());
        assertInstanceOf(ResolverError.Missing.class, r.unwrapErr());
        assertNotNull(r.unwrapErr());
    }
}
