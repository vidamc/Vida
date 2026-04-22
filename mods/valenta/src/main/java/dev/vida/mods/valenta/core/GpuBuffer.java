/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.core;

import dev.vida.core.ApiStatus;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Typed wrapper around an OpenGL buffer object (VBO, SSBO, etc.).
 *
 * <p>Manages lifecycle (create/upload/delete) and tracks capacity to avoid
 * redundant re-allocations. All methods must be called on the render thread.
 *
 * <p>The actual GL calls are delegated to {@link GlFunctions} for testability;
 * in headless tests a no-op stub is injected.
 */
@ApiStatus.Preview("valenta")
public final class GpuBuffer implements AutoCloseable {

    /** OpenGL buffer target constants. */
    public enum Target {
        ARRAY_BUFFER(0x8892),
        ELEMENT_ARRAY_BUFFER(0x8893),
        SHADER_STORAGE_BUFFER(0x90D2),
        DRAW_INDIRECT_BUFFER(0x8F3F);

        private final int glEnum;
        Target(int glEnum) { this.glEnum = glEnum; }
        public int gl() { return glEnum; }
    }

    /** Usage hint constants. */
    public enum Usage {
        STATIC_DRAW(0x88E4),
        DYNAMIC_DRAW(0x88E8),
        STREAM_DRAW(0x88E0);

        private final int glEnum;
        Usage(int glEnum) { this.glEnum = glEnum; }
        public int gl() { return glEnum; }
    }

    private final Target target;
    private final Usage usage;
    private final GlFunctions gl;
    private int id;
    private long capacityBytes;
    private boolean closed;

    /**
     * Creates a new GPU buffer.
     *
     * @param target GL target (e.g. ARRAY_BUFFER)
     * @param usage  GL usage hint
     * @param gl     GL function provider (injectable for tests)
     */
    public GpuBuffer(Target target, Usage usage, GlFunctions gl) {
        this.target = Objects.requireNonNull(target, "target");
        this.usage = Objects.requireNonNull(usage, "usage");
        this.gl = Objects.requireNonNull(gl, "gl");
        this.id = gl.glGenBuffers();
    }

    /**
     * Uploads data. Re-allocates only when the new data exceeds current capacity.
     *
     * @param data the data buffer (position to limit is uploaded)
     */
    @ApiStatus.HotPath
    public void upload(ByteBuffer data) {
        ensureOpen();
        Objects.requireNonNull(data, "data");
        long needed = data.remaining();
        gl.glBindBuffer(target.gl(), id);
        if (needed > capacityBytes) {
            long newCapacity = alignUp(needed);
            gl.glBufferData(target.gl(), newCapacity, usage.gl());
            capacityBytes = newCapacity;
        }
        gl.glBufferSubData(target.gl(), 0, data);
        gl.glBindBuffer(target.gl(), 0);
    }

    /** Current capacity in bytes. */
    public long capacityBytes() {
        return capacityBytes;
    }

    /** OpenGL buffer ID. */
    public int id() {
        ensureOpen();
        return id;
    }

    public Target target() { return target; }

    public boolean isClosed() { return closed; }

    @Override
    public void close() {
        if (!closed) {
            gl.glDeleteBuffers(id);
            id = 0;
            capacityBytes = 0;
            closed = true;
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("GpuBuffer already closed");
        }
    }

    /**
     * Rounds up to the next power of two (min 4096) to amortize re-allocations.
     */
    static long alignUp(long bytes) {
        long min = 4096L;
        if (bytes <= min) return min;
        long p = Long.highestOneBit(bytes);
        return (p == bytes) ? p : p << 1;
    }
}
