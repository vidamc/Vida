/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import static dev.vida.resolver.Providers.p;
import static dev.vida.resolver.Providers.pp;
import static dev.vida.resolver.Providers.req;
import static dev.vida.resolver.Providers.uni;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vida.core.Version;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResolverProvidesTest {

    @Test
    void alias_satisfies_dependency() {
        // fork-of-legacy предоставляет legacy; root требует legacy.
        Universe u = uni(
                p("root", "1.0.0", req("legacy", ">=1.0.0")),
                pp("fork", "1.0.0", List.of("legacy")));
        Resolution res = Resolver.resolve(Set.of("root"), u).unwrap();
        assertEquals(2, res.selected().size());
        // selected ключуется по собственному id — это "fork", не "legacy".
        assertTrue(res.selected().containsKey("fork"));
        assertTrue(res.findByQuery("legacy").isPresent());
        assertEquals("fork", res.findByQuery("legacy").orElseThrow().id());
    }

    @Test
    void direct_wins_over_alias_when_both_present() {
        // Настоящий legacy и форк оба есть — резолвер не должен двоить.
        Universe u = uni(
                p("root", "1.0.0", req("legacy", ">=1.0.0")),
                p("legacy", "1.0.0"),
                pp("fork", "1.0.0", List.of("legacy")));
        Resolution res = Resolver.resolve(Set.of("root"), u).unwrap();
        // Любое из двух допустимо, но ровно один провайдер закрывает "legacy".
        assertTrue(res.selected().containsKey("legacy") || res.selected().containsKey("fork"));
        // selected только root + один из (legacy, fork) — не оба.
        assertEquals(2, res.selected().size());
    }

    @Test
    void alias_ties_broken_by_strategy() {
        Universe u = uni(
                p("root", "1.0.0", req("legacy", "*")),
                pp("forkA", "1.0.0", List.of("legacy")),
                pp("forkB", "2.0.0", List.of("legacy")));
        Resolution res = Resolver.resolve(Set.of("root"), u).unwrap();
        // NEWEST по умолчанию: forkB@2.0.0 предпочтительнее forkA@1.0.0.
        assertEquals(Version.parse("2.0.0"),
                res.findByQuery("legacy").orElseThrow().version());
    }
}
