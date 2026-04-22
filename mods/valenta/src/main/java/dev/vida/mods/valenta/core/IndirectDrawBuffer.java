/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.core;

import dev.vida.core.ApiStatus;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * CPU-side buffer for {@code GL_DRAW_INDIRECT_BUFFER} commands.
 *
 * <p>Each command is a {@code DrawElementsIndirectCommand} struct (20 bytes):
 * <pre>
 *   uint count;           // index count per draw
 *   uint instanceCount;   // always 1
 *   uint firstIndex;      // offset into EBO
 *   int  baseVertex;      // added to each index
 *   uint baseInstance;    // per-instance data offset
 * </pre>
 *
 * <p>After populating commands via {@link #addCommand}, the internal buffer is
 * uploaded to the GPU via {@link #upload(GpuBuffer)} for use with
 * {@code glMultiDrawElementsIndirect}.
 */
@ApiStatus.Preview("valenta")
public final class IndirectDrawBuffer {

    public static final int COMMAND_STRIDE = 20;
    private static final int INITIAL_CAPACITY = 256;

    private ByteBuffer buffer;
    private int commandCount;

    public IndirectDrawBuffer() {
        this.buffer = ByteBuffer.allocateDirect(INITIAL_CAPACITY * COMMAND_STRIDE)
                .order(ByteOrder.nativeOrder());
    }

    /**
     * Resets the buffer for a new frame without deallocating.
     */
    @ApiStatus.HotPath
    public void reset() {
        buffer.clear();
        commandCount = 0;
    }

    /**
     * Appends one draw command.
     *
     * @param indexCount    number of indices for this draw
     * @param firstIndex   byte offset into the element buffer
     * @param baseVertex   value added to each index
     * @param baseInstance per-instance data offset (used for section ID)
     */
    @ApiStatus.HotPath
    public void addCommand(int indexCount, int firstIndex, int baseVertex, int baseInstance) {
        ensureCapacity(1);
        buffer.putInt(indexCount);
        buffer.putInt(1);
        buffer.putInt(firstIndex);
        buffer.putInt(baseVertex);
        buffer.putInt(baseInstance);
        commandCount++;
    }

    /** Number of commands currently in the buffer. */
    public int commandCount() {
        return commandCount;
    }

    /**
     * Returns the internal byte buffer with position 0 and limit set to the
     * end of the last command. Suitable for upload.
     */
    public ByteBuffer asByteBuffer() {
        buffer.flip();
        return buffer;
    }

    /**
     * Uploads the command buffer to the given GPU buffer.
     *
     * @param gpuBuf a buffer with target {@code DRAW_INDIRECT_BUFFER}
     */
    public void upload(GpuBuffer gpuBuf) {
        Objects.requireNonNull(gpuBuf, "gpuBuf");
        ByteBuffer slice = asByteBuffer();
        gpuBuf.upload(slice);
        buffer.clear();
        buffer.position(commandCount * COMMAND_STRIDE);
    }

    /** Total capacity in commands. */
    public int capacity() {
        return buffer.capacity() / COMMAND_STRIDE;
    }

    private void ensureCapacity(int additional) {
        int needed = (commandCount + additional) * COMMAND_STRIDE;
        if (needed > buffer.capacity()) {
            int newCap = Math.max(buffer.capacity() * 2, needed);
            ByteBuffer grown = ByteBuffer.allocateDirect(newCap).order(ByteOrder.nativeOrder());
            buffer.flip();
            grown.put(buffer);
            buffer = grown;
        }
    }
}
