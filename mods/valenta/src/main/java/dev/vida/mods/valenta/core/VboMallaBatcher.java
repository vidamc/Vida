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
 * VBO batcher that groups per-section vertex data into a single large VBO
 * and issues one {@code glMultiDrawElementsIndirect} call per render pass.
 *
 * <h2>Pipeline overview</h2>
 * <ol>
 *   <li>For each visible chunk section, the meshing stage fills a
 *       {@link dev.vida.mods.valenta.chunk.MallaChunk MallaChunk}.</li>
 *   <li>The upload stage calls {@link #appendSection} to copy vertex data
 *       into the mega-VBO and record an indirect draw command.</li>
 *   <li>{@link #flush()} uploads the indirect command buffer and executes
 *       {@code glMultiDrawElementsIndirect}.</li>
 * </ol>
 *
 * <h2>Performance</h2>
 * Reducing thousands of individual draw calls to a single multi-draw
 * eliminates CPU-side GL driver overhead and enables the GPU to schedule
 * work more efficiently.
 */
@ApiStatus.Preview("valenta")
public final class VboMallaBatcher implements AutoCloseable {

    private static final int GL_TRIANGLES = 0x0004;
    private static final int GL_UNSIGNED_INT = 0x1405;

    private final GpuBuffer vertexBuffer;
    private final GpuBuffer indexBuffer;
    private final GpuBuffer indirectBuffer;
    private final IndirectDrawBuffer commands;
    private final GlFunctions gl;

    private ByteBuffer stagingVertices;
    private ByteBuffer stagingIndices;
    private int totalVertices;
    private int totalIndices;
    private boolean closed;

    /**
     * @param gl            GL function provider
     * @param initialVerts  initial vertex staging capacity (vertex count)
     * @param initialIdxs   initial index staging capacity (index count)
     */
    public VboMallaBatcher(GlFunctions gl, int initialVerts, int initialIdxs) {
        this.gl = Objects.requireNonNull(gl, "gl");
        this.vertexBuffer = new GpuBuffer(
                GpuBuffer.Target.ARRAY_BUFFER, GpuBuffer.Usage.DYNAMIC_DRAW, gl);
        this.indexBuffer = new GpuBuffer(
                GpuBuffer.Target.ELEMENT_ARRAY_BUFFER, GpuBuffer.Usage.DYNAMIC_DRAW, gl);
        this.indirectBuffer = new GpuBuffer(
                GpuBuffer.Target.DRAW_INDIRECT_BUFFER, GpuBuffer.Usage.DYNAMIC_DRAW, gl);
        this.commands = new IndirectDrawBuffer();
        this.stagingVertices = ByteBuffer.allocateDirect(
                initialVerts * CompactVertexFormat.BYTES_PER_VERTEX).order(ByteOrder.nativeOrder());
        this.stagingIndices = ByteBuffer.allocateDirect(
                initialIdxs * 4).order(ByteOrder.nativeOrder());
    }

    /**
     * Resets state for a new frame. Does not deallocate GPU memory.
     */
    @ApiStatus.HotPath
    public void beginFrame() {
        ensureOpen();
        stagingVertices.clear();
        stagingIndices.clear();
        commands.reset();
        totalVertices = 0;
        totalIndices = 0;
    }

    /**
     * Appends vertex + index data for one chunk section and records
     * an indirect draw command.
     *
     * @param vertices vertex data in compact format
     * @param indices  index data (uint32 each)
     * @param vertexCount  number of vertices
     * @param indexCount    number of indices
     * @param sectionId    unique section identifier for per-instance data
     */
    @ApiStatus.HotPath
    public void appendSection(ByteBuffer vertices, ByteBuffer indices,
                              int vertexCount, int indexCount, int sectionId) {
        ensureOpen();
        ensureStagingCapacity(vertexCount, indexCount);

        int baseVertex = totalVertices;
        int firstIndex = totalIndices;

        stagingVertices.put(vertices);
        stagingIndices.put(indices);

        commands.addCommand(indexCount, firstIndex, baseVertex, sectionId);
        totalVertices += vertexCount;
        totalIndices += indexCount;
    }

    /**
     * Uploads all staged data and issues the multi-draw call.
     *
     * @return number of draw commands executed
     */
    @ApiStatus.HotPath
    public int flush() {
        ensureOpen();
        int count = commands.commandCount();
        if (count == 0) return 0;

        stagingVertices.flip();
        vertexBuffer.upload(stagingVertices);

        stagingIndices.flip();
        indexBuffer.upload(stagingIndices);

        commands.upload(indirectBuffer);

        gl.glBindBuffer(GpuBuffer.Target.ARRAY_BUFFER.gl(), vertexBuffer.id());
        gl.glBindBuffer(GpuBuffer.Target.ELEMENT_ARRAY_BUFFER.gl(), indexBuffer.id());
        gl.glBindBuffer(GpuBuffer.Target.DRAW_INDIRECT_BUFFER.gl(), indirectBuffer.id());

        gl.glMultiDrawElementsIndirect(
                GL_TRIANGLES, GL_UNSIGNED_INT, 0L, count, IndirectDrawBuffer.COMMAND_STRIDE);

        gl.glBindBuffer(GpuBuffer.Target.DRAW_INDIRECT_BUFFER.gl(), 0);
        gl.glBindBuffer(GpuBuffer.Target.ELEMENT_ARRAY_BUFFER.gl(), 0);
        gl.glBindBuffer(GpuBuffer.Target.ARRAY_BUFFER.gl(), 0);

        return count;
    }

    public int totalVertices() { return totalVertices; }
    public int totalIndices() { return totalIndices; }
    public int commandCount() { return commands.commandCount(); }

    @Override
    public void close() {
        if (!closed) {
            vertexBuffer.close();
            indexBuffer.close();
            indirectBuffer.close();
            closed = true;
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("VboMallaBatcher already closed");
    }

    private void ensureStagingCapacity(int addVerts, int addIdxs) {
        int neededVBytes = (totalVertices + addVerts) * CompactVertexFormat.BYTES_PER_VERTEX;
        if (neededVBytes > stagingVertices.capacity()) {
            stagingVertices = grow(stagingVertices, neededVBytes);
        }
        int neededIBytes = (totalIndices + addIdxs) * 4;
        if (neededIBytes > stagingIndices.capacity()) {
            stagingIndices = grow(stagingIndices, neededIBytes);
        }
    }

    private static ByteBuffer grow(ByteBuffer old, int needed) {
        int newCap = Math.max(old.capacity() * 2, needed);
        ByteBuffer grown = ByteBuffer.allocateDirect(newCap).order(ByteOrder.nativeOrder());
        old.flip();
        grown.put(old);
        return grown;
    }
}
