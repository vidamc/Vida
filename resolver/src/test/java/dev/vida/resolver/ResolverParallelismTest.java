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
import org.junit.jupiter.api.RepeatedTest;

/** Параллельная подготовка провайдеров не меняет итог резолюции (детерминизм). */
final class ResolverParallelismTest {

    @RepeatedTest(5)
    void parallel_indexed_providers_match_sequential() {
        int n = 64;
        List<Provider> sequential = IntStream.range(0, n)
                .mapToObj(i -> Provider.builder("m" + i, Version.parse("1.0.0")).build())
                .toList();
        Universe.Builder ubSeq = Universe.builder();
        Set<String> rootsSeq = new LinkedHashSet<>();
        for (Provider p : sequential) {
            ubSeq.add(p);
            rootsSeq.add(p.id());
        }
        Result<Resolution, ResolverError> rSeq =
                Resolver.resolve(rootsSeq, ubSeq.build(), ResolverOptions.DEFAULTS);

        List<java.util.Map.Entry<Integer, Provider>> indexed =
                IntStream.range(0, n)
                        .parallel()
                        .mapToObj(i -> java.util.Map.entry(
                                i,
                                Provider.builder("m" + i, Version.parse("1.0.0")).build()))
                        .sorted(java.util.Comparator.comparingInt(java.util.Map.Entry::getKey))
                        .toList();
        Universe.Builder ubPar = Universe.builder();
        Set<String> rootsPar = new LinkedHashSet<>();
        for (var e : indexed) {
            ubPar.add(e.getValue());
            rootsPar.add(e.getValue().id());
        }
        Result<Resolution, ResolverError> rPar =
                Resolver.resolve(rootsPar, ubPar.build(), ResolverOptions.DEFAULTS);

        assertThat(rPar.isOk()).isTrue();
        assertThat(rSeq.isOk()).isTrue();
        assertThat(rPar.unwrap().selected()).isEqualTo(rSeq.unwrap().selected());
    }
}
