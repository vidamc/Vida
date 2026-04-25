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
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vida.core.Result;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ResolverAccessPolicyTest {

    @Test
    void denied_root_fails_fast() {
        Universe u = uni(p("bad", "1.0.0"));
        ResolverOptions opt = ResolverOptions.DEFAULTS.withAccessDenied("bad");
        Result<Resolution, ResolverError> r = Resolver.resolve(Set.of("bad"), u, opt);
        assertTrue(r.isErr());
        assertInstanceOf(ResolverError.AccessPolicyDenied.class, r.unwrapErr());
        ResolverError.AccessPolicyDenied e = (ResolverError.AccessPolicyDenied) r.unwrapErr();
        assertEquals("bad", e.id());
        assertEquals("root mod blocked by access policy (accessDeniedIds)", e.detail());
    }

    @Test
    void denied_required_dependency_fails_with_access_policy_denied() {
        Universe u = uni(
                p("root", "1.0.0", req("lib", ">=1.0.0")),
                p("lib", "1.0.0"));
        ResolverOptions opt = ResolverOptions.DEFAULTS.withAccessDenied("lib");
        Result<Resolution, ResolverError> r = Resolver.resolve(Set.of("root"), u, opt);
        assertTrue(r.isErr());
        assertInstanceOf(ResolverError.AccessPolicyDenied.class, r.unwrapErr());
    }

    @Test
    void all_versions_of_denied_id_fail_even_when_multiple_providers_exist() {
        Universe u = uni(
                p("root", "1.0.0", req("lib", ">=1.0.0")),
                p("lib", "1.0.0"),
                p("lib", "2.0.0"));
        ResolverOptions opt = ResolverOptions.DEFAULTS.withAccessDenied("lib");
        Result<Resolution, ResolverError> r = Resolver.resolve(Set.of("root"), u, opt);
        assertTrue(r.isErr());
        assertInstanceOf(ResolverError.AccessPolicyDenied.class, r.unwrapErr());
    }

    @Test
    void resolves_when_policy_does_not_apply() {
        Universe u = uni(
                p("root", "1.0.0", req("lib", ">=1.0.0")),
                p("lib", "1.0.0"));
        ResolverOptions opt = ResolverOptions.DEFAULTS.withAccessDenied("other");
        Resolution res = Resolver.resolve(Set.of("root"), u, opt).unwrap();
        assertEquals(2, res.selected().size());
    }
}
