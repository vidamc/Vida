/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada.jmh;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Синтетика: повторяющийся поиск ключа в кеше разрешения методов (как в
 * {@code MorphApplier} после доработки).
 */
@Fork(1)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class MorphApplierCacheHarness {

    @State(Scope.Thread)
    public static class Ctx {
        final Map<String, String> cache = new HashMap<>();
        {
            for (int i = 0; i < 200; i++) {
                cache.put("m" + i + "\0" + "(I)V", "hit");
            }
        }
        final String key = "m3\0(I)V";
    }

    @Benchmark
    public void cacheGet(Blackhole bh, Ctx ctx) {
        bh.consume(ctx.cache.get(ctx.key));
    }
}
