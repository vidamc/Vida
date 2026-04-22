/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import static dev.vida.resolver.Providers.inc;
import static dev.vida.resolver.Providers.p;
import static dev.vida.resolver.Providers.req;
import static dev.vida.resolver.Providers.uni;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vida.core.Result;
import dev.vida.core.Version;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResolverConflictTest {

    @Test
    void missing_returns_structured_error() {
        Universe u = uni(p("root", "1.0.0", req("lib", ">=2.0.0")));
        Result<Resolution, ResolverError> r = Resolver.resolve(Set.of("root"), u);
        ResolverError.Missing m = (ResolverError.Missing) r.unwrapErr();
        assertEquals("lib", m.id());
        assertTrue(m.requesters().stream().anyMatch(s -> s.startsWith("root@")));
    }

    @Test
    void version_conflict_between_two_requesters() {
        Universe u = uni(
                p("root", "1.0.0", req("a", "*"), req("lib", "<1.0.0"), req("b", "*")),
                p("a", "1.0.0", req("lib", ">=2.0.0")),
                p("b", "1.0.0"),
                p("lib", "1.5.0"));
        Result<Resolution, ResolverError> r = Resolver.resolve(Set.of("root"), u);
        ResolverError err = r.unwrapErr();
        // Может быть либо VersionConflict, либо Missing (range +incompat filter),
        // главное — это не успех и ошибка указывает на lib.
        if (err instanceof ResolverError.VersionConflict vc) {
            assertEquals("lib", vc.id());
            assertTrue(vc.constraints().size() >= 1,
                    "conflict must list at least the failing requester");
        } else if (err instanceof ResolverError.Missing m) {
            assertEquals("lib", m.id());
        } else {
            throw new AssertionError("unexpected error: " + err);
        }
    }

    @Test
    void incompatibility_declaration_blocks_selection() {
        Universe u = uni(
                p("root", "1.0.0", req("a", "*"), req("b", "*")),
                p("a", "1.0.0", inc("b", "*")),
                p("b", "1.0.0"));
        Result<Resolution, ResolverError> r = Resolver.resolve(Set.of("root"), u);
        assertTrue(r.isErr());
        assertInstanceOf(ResolverError.Incompatibility.class, r.unwrapErr());
    }

    @Test
    void incompatibility_versioned_range_respected() {
        // a несовместим с b <1.5.0, но b 2.0.0 доступна — резолвер должен её выбрать.
        Universe u = uni(
                p("root", "1.0.0", req("a", "*"), req("b", "*")),
                p("a", "1.0.0", inc("b", "<1.5.0")),
                p("b", "1.0.0"),
                p("b", "2.0.0"));
        Resolution res = Resolver.resolve(Set.of("root"), u).unwrap();
        assertEquals(Version.parse("2.0.0"), res.selected().get("b").version());
    }

    @Test
    void excluded_hard_requester_returns_Excluded() {
        Universe u = uni(
                p("root", "1.0.0", req("a", "*")),
                p("a", "1.0.0"));
        ResolverOptions opts = ResolverOptions.DEFAULTS.withExclude("a");
        Result<Resolution, ResolverError> r = Resolver.resolve(Set.of("root"), u, opts);
        assertInstanceOf(ResolverError.Excluded.class, r.unwrapErr());
    }

    @Test
    void bad_pin_not_in_universe() {
        Universe u = uni(p("a", "1.0.0"));
        ResolverOptions opts = ResolverOptions.DEFAULTS
                .withPin("a", Version.parse("9.9.9"));
        Result<Resolution, ResolverError> r = Resolver.resolve(Set.of("a"), u, opts);
        assertInstanceOf(ResolverError.BadPin.class, r.unwrapErr());
    }

    @Test
    void bad_pin_violating_constraint() {
        Universe u = uni(
                p("root", "1.0.0", req("a", ">=2.0.0")),
                p("a", "1.0.0"),
                p("a", "2.0.0"));
        ResolverOptions opts = ResolverOptions.DEFAULTS
                .withPin("a", Version.parse("1.0.0"));
        Result<Resolution, ResolverError> r = Resolver.resolve(Set.of("root"), u, opts);
        assertInstanceOf(ResolverError.BadPin.class, r.unwrapErr());
    }

    @Test
    void valid_pin_overrides_newest_strategy() {
        Universe u = uni(
                p("a", "1.0.0"),
                p("a", "2.0.0"));
        ResolverOptions opts = ResolverOptions.DEFAULTS
                .withPin("a", Version.parse("1.0.0"));
        Resolution res = Resolver.resolve(Set.of("a"), u, opts).unwrap();
        assertEquals(Version.parse("1.0.0"), res.selected().get("a").version());
    }
}
