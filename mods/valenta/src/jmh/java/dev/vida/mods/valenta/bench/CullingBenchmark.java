/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.bench;

import dev.vida.mods.valenta.culling.ValentaFrustum;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmarks for frustum culling throughput.
 *
 * <p>Simulates testing 10 000 chunk sections per frame, which
 * represents render distance ~16 in all directions.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class CullingBenchmark {

    private ValentaFrustum frustum;

    @Setup
    public void setup() {
        frustum = new ValentaFrustum();
        frustum.update(new double[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        });
    }

    @Benchmark
    public int testSections10000() {
        int visible = 0;
        for (int x = -16; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = -16; z < 16; z++) {
                    if (frustum.testSection(x, y, z)) visible++;
                }
            }
        }
        return visible;
    }

    @Benchmark
    public boolean singleAabbTest() {
        return frustum.testAabb(0, 0, 0, 16, 16, 16);
    }
}
