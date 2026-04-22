/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.bench;

import dev.vida.mods.valenta.core.CompactVertexFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
 * Benchmarks for compact vertex format encode/decode throughput.
 *
 * <p>Run: {@code java -jar build/libs/valenta-jmh.jar VertexFormatBenchmark}
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class VertexFormatBenchmark {

    private ByteBuffer buffer;

    @Setup
    public void setup() {
        buffer = ByteBuffer.allocateDirect(1024 * CompactVertexFormat.BYTES_PER_VERTEX)
                .order(ByteOrder.nativeOrder());
    }

    @Benchmark
    public void encode1024Vertices() {
        for (int i = 0; i < 1024; i++) {
            int offset = i * CompactVertexFormat.BYTES_PER_VERTEX;
            CompactVertexFormat.encode(buffer, offset,
                    (i % 16), (i / 16 % 16), (i / 256 % 16),
                    0, 1, 0xFFFFFFFF, 0.0f, 0.0f);
        }
    }

    @Benchmark
    public float decode1024Vertices() {
        float sum = 0;
        for (int i = 0; i < 1024; i++) {
            int offset = i * CompactVertexFormat.BYTES_PER_VERTEX;
            sum += CompactVertexFormat.decodeX(buffer, offset);
            sum += CompactVertexFormat.decodeY(buffer, offset);
            sum += CompactVertexFormat.decodeZ(buffer, offset);
        }
        return sum;
    }
}
