/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.bench;

import dev.vida.mods.valenta.chunk.AnalisisEtapa;
import dev.vida.mods.valenta.chunk.BuildEtapa;
import dev.vida.mods.valenta.chunk.MallaChunk;
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
 * Benchmarks for chunk meshing pipeline.
 *
 * <p>Compares meshing throughput for different section fill patterns:
 * empty, sparse (10% fill), dense (90% fill), and solid (100% fill).
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class MeshingBenchmark {

    private BuildEtapa build;
    private AnalisisEtapa.SectionRequest request;
    private BuildEtapa.SectionSnapshot sparseSnapshot;
    private BuildEtapa.SectionSnapshot denseSnapshot;
    private BuildEtapa.SectionSnapshot solidSnapshot;

    @Setup
    public void setup() {
        build = new BuildEtapa(false, false);
        request = new AnalisisEtapa.SectionRequest(0, 4, 0, 100.0, 1);

        sparseSnapshot = new PatternSnapshot(0.1);
        denseSnapshot = new PatternSnapshot(0.9);
        solidSnapshot = new PatternSnapshot(1.0);
    }

    @Benchmark
    public MallaChunk meshSparseSection() {
        return build.construir(request, sparseSnapshot);
    }

    @Benchmark
    public MallaChunk meshDenseSection() {
        return build.construir(request, denseSnapshot);
    }

    @Benchmark
    public MallaChunk meshSolidSection() {
        return build.construir(request, solidSnapshot);
    }

    private static final class PatternSnapshot implements BuildEtapa.SectionSnapshot {
        private final double fillRatio;
        PatternSnapshot(double fillRatio) { this.fillRatio = fillRatio; }

        @Override
        public int blockStateAt(int x, int y, int z) {
            return (hashCoord(x, y, z) & 0xFF) < (int) (fillRatio * 256) ? 1 : 0;
        }

        @Override
        public boolean isOpaque(int x, int y, int z) {
            return blockStateAt(x, y, z) != 0;
        }

        @Override public int biomeColor(int x, int z) { return 0xFF00FF00; }
        @Override public int lightAt(int x, int y, int z) { return 0xFF; }

        private static int hashCoord(int x, int y, int z) {
            int h = x * 73856093 ^ y * 19349663 ^ z * 83492791;
            return h & 0x7FFFFFFF;
        }
    }
}
