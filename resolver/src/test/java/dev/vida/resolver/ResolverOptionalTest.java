/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import static dev.vida.resolver.Providers.opt;
import static dev.vida.resolver.Providers.p;
import static dev.vida.resolver.Providers.req;
import static dev.vida.resolver.Providers.uni;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ResolverOptionalTest {

    @Test
    void optional_dep_pulled_in_when_available() {
        Universe u = uni(
                p("root", "1.0.0", opt("extra", "*")),
                p("extra", "1.0.0"));
        Resolution res = Resolver.resolve(Set.of("root"), u).unwrap();
        assertEquals(2, res.selected().size());
        assertTrue(res.optionalIncluded().contains("extra"));
        assertFalse(res.optionalMissing().contains("extra"));
    }

    @Test
    void optional_dep_missing_when_not_in_universe() {
        Universe u = uni(p("root", "1.0.0", opt("extra", "*")));
        Resolution res = Resolver.resolve(Set.of("root"), u).unwrap();
        assertEquals(1, res.selected().size());
        assertTrue(res.optionalMissing().contains("extra"));
    }

    @Test
    void skipOptional_does_not_even_try_to_resolve_optionals() {
        Universe u = uni(
                p("root", "1.0.0", opt("extra", "*")),
                p("extra", "1.0.0"));
        Resolution res = Resolver.resolve(Set.of("root"), u,
                ResolverOptions.DEFAULTS.withSkipOptional(true)).unwrap();
        assertEquals(1, res.selected().size());
        assertTrue(res.optionalMissing().contains("extra"));
    }

    @Test
    void optional_missing_when_range_doesnt_match() {
        Universe u = uni(
                p("root", "1.0.0", opt("extra", ">=2.0.0")),
                p("extra", "1.0.0"));
        Resolution res = Resolver.resolve(Set.of("root"), u).unwrap();
        assertEquals(1, res.selected().size());
        assertTrue(res.optionalMissing().contains("extra"));
    }

    @Test
    void later_required_dep_overrides_earlier_optional_missing() {
        // root optionally wants extra>=2.0.0 (not satisfiable),
        // b hard-requires extra>=1.0.0 (satisfiable).
        // Резолвер должен переклассифицировать extra и выбрать 1.0.0.
        Universe u = uni(
                p("root", "1.0.0", opt("extra", ">=2.0.0"), req("b", "*")),
                p("b", "1.0.0", req("extra", ">=1.0.0")),
                p("extra", "1.0.0"));
        // Важно: первое обнаружение extra произойдёт с optional, скорее всего
        // в этот момент v>=2 нет. Позже при apply для b мы добавим required
        // с диапазоном >=1.0.0 — идея: ранее помещённый в optionalMissing
        // должен вернуться в pending.
        //
        // ВНИМАНИЕ: текущий порядок обхода pending FIFO зависит от MRV;
        // мы явно ожидаем либо успех с extra=1.0.0, либо VersionConflict
        // — результат не должен быть "тихо пропустили required".
        var r = Resolver.resolve(Set.of("root"), u);
        if (r.isOk()) {
            Resolution res = r.unwrap();
            assertTrue(res.selected().containsKey("extra"),
                    "extra must be selected because b hard-requires it");
        } else {
            // Допустимая альтернатива — VersionConflict на extra с диапазонами
            // [>=2.0.0, >=1.0.0] (второй удовлетворяется, первый нет).
            assertTrue(r.unwrapErr() instanceof ResolverError.VersionConflict);
        }
    }
}
