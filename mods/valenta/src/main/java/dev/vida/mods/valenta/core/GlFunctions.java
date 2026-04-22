/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.core;

import dev.vida.core.ApiStatus;
import java.nio.ByteBuffer;

/**
 * Abstraction over OpenGL calls, injectable for headless testing.
 *
 * <p>Each method mirrors the corresponding GL function. The runtime
 * implementation delegates to LWJGL/Blaze3D; in tests, {@link Noop}
 * or a recording mock can be used.
 */
@ApiStatus.Preview("valenta")
public interface GlFunctions {

    int glGenBuffers();

    void glDeleteBuffers(int id);

    void glBindBuffer(int target, int id);

    void glBufferData(int target, long size, int usage);

    void glBufferSubData(int target, long offset, ByteBuffer data);

    void glMultiDrawElementsIndirect(int mode, int type, long indirect, int drawCount, int stride);

    int glGenQueries();

    void glDeleteQueries(int id);

    void glBeginQuery(int target, int id);

    void glEndQuery(int target);

    /**
     * @return number of samples passed (0 = fully occluded)
     */
    int glGetQueryObjectiv(int id, int pname);

    void glBeginConditionalRender(int id, int mode);

    void glEndConditionalRender();

    void glInvalidateFramebuffer(int target, int attachment);

    /**
     * No-op implementation for headless testing. All methods return safe defaults;
     * buffer IDs are sequential monotonic integers.
     */
    @ApiStatus.Internal
    final class Noop implements GlFunctions {
        private int nextId = 1;
        @Override public int glGenBuffers() { return nextId++; }
        @Override public void glDeleteBuffers(int id) {}
        @Override public void glBindBuffer(int target, int id) {}
        @Override public void glBufferData(int target, long size, int usage) {}
        @Override public void glBufferSubData(int target, long offset, ByteBuffer data) {}
        @Override public void glMultiDrawElementsIndirect(int mode, int type, long indirect, int drawCount, int stride) {}
        @Override public int glGenQueries() { return nextId++; }
        @Override public void glDeleteQueries(int id) {}
        @Override public void glBeginQuery(int target, int id) {}
        @Override public void glEndQuery(int target) {}
        @Override public int glGetQueryObjectiv(int id, int pname) { return 0; }
        @Override public void glBeginConditionalRender(int id, int mode) {}
        @Override public void glEndConditionalRender() {}
        @Override public void glInvalidateFramebuffer(int target, int attachment) {}
    }
}
