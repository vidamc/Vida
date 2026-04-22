/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import static dev.vida.resolver.Providers.p;
import static dev.vida.resolver.Providers.req;
import static dev.vida.resolver.Providers.uni;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vida.core.Version;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ResolverBacktrackingTest {

    @Test
    void backtracks_when_newest_path_dead_ends() {
        // root требует lib и ext;
        // lib@2 требует util>=2, lib@1 требует util>=1;
        // util есть только v1.0.0.
        // NEWEST сначала попробует lib@2, упадёт по util, вернётся и возьмёт lib@1.
        Universe u = uni(
                p("root", "1.0.0", req("lib", "*"), req("ext", "*")),
                p("lib", "1.0.0", req("util", ">=1.0.0")),
                p("lib", "2.0.0", req("util", ">=2.0.0")),
                p("util", "1.0.0"),
                p("ext", "1.0.0"));
        Resolution res = Resolver.resolve(Set.of("root"), u).unwrap();
        assertEquals(Version.parse("1.0.0"), res.selected().get("lib").version());
        assertEquals(Version.parse("1.0.0"), res.selected().get("util").version());
        assertTrue(res.decisions() >= 2, "должно быть >= 2 decisions для backtrack");
    }

    @Test
    void diamond_dependency_resolves() {
        // A → B, A → C, B → D>=1, C → D>=1; универс содержит D=1.0 и D=2.0.
        Universe u = uni(
                p("A", "1.0.0", req("B", "*"), req("C", "*")),
                p("B", "1.0.0", req("D", ">=1.0.0")),
                p("C", "1.0.0", req("D", ">=1.0.0")),
                p("D", "1.0.0"),
                p("D", "2.0.0"));
        Resolution res = Resolver.resolve(Set.of("A"), u).unwrap();
        assertEquals(4, res.selected().size());
        // NEWEST выберет D=2.0.0.
        assertEquals(Version.parse("2.0.0"), res.selected().get("D").version());
    }

    @Test
    void conflicting_diamond_forces_older_D() {
        Universe u = uni(
                p("A", "1.0.0", req("B", "*"), req("C", "*")),
                p("B", "1.0.0", req("D", ">=1.0.0 <2.0.0")),
                p("C", "1.0.0", req("D", ">=1.0.0")),
                p("D", "1.0.0"),
                p("D", "2.0.0"));
        Resolution res = Resolver.resolve(Set.of("A"), u).unwrap();
        assertEquals(Version.parse("1.0.0"), res.selected().get("D").version());
    }
}
