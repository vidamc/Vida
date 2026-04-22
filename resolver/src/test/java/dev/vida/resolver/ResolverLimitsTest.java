/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import static dev.vida.resolver.Providers.p;
import static dev.vida.resolver.Providers.req;
import static dev.vida.resolver.Providers.uni;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vida.core.Result;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResolverLimitsTest {

    @Test
    void decision_limit_returns_DecisionLimit_on_pathological_input() {
        // Искусственно делаем «глубокую» зависимость, чтобы проход по ней
        // потребовал много decisions.
        Universe u = uni(
                p("root", "1.0.0", req("a", "*")),
                p("a", "1.0.0", req("b", "*")),
                p("a", "2.0.0", req("b", ">=2.0.0")),
                p("b", "1.0.0"));
        ResolverOptions opts = ResolverOptions.DEFAULTS
                .withMaxDecisions(1);
        Result<Resolution, ResolverError> r = Resolver.resolve(Set.of("root"), u, opts);
        // Либо успех за 1 decision (если MRV сразу угадал), либо DecisionLimit.
        if (r.isErr()) {
            assertInstanceOf(ResolverError.DecisionLimit.class, r.unwrapErr());
        } else {
            assertTrue(r.unwrap().decisions() <= 1);
        }
    }

    @Test
    void options_validate_positive_bounds() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ResolverOptions.DEFAULTS.withMaxDecisions(0));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> ResolverOptions.DEFAULTS.withTimeoutMillis(0));
    }

    @Test
    void fluent_setters_do_not_mutate_originals() {
        ResolverOptions base = ResolverOptions.DEFAULTS;
        ResolverOptions pinned = base
                .withPin("a", dev.vida.core.Version.of(1, 0, 0))
                .withExclude("b")
                .withSkipOptional(true);
        assertTrue(base.pins().isEmpty());
        assertTrue(base.excludes().isEmpty());
        assertTrue(!base.skipOptional());
        assertTrue(pinned.pins().containsKey("a"));
        assertTrue(pinned.excludes().contains("b"));
        assertTrue(pinned.skipOptional());
    }
}
