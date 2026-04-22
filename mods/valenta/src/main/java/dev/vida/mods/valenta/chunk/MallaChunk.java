/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.chunk;

import dev.vida.core.ApiStatus;
import dev.vida.mods.valenta.core.CompactVertexFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Immutable mesh data for a single 16×16×16 chunk section.
 *
 * <p>Produced by {@link BuildEtapa} on a worker thread; consumed by
 * {@link UploadEtapa} on the render thread. The vertex data uses
 * {@link CompactVertexFormat}.
 *
 * <h2>Invariants</h2>
 * <ul>
 *   <li>{@code vertexCount > 0} (empty sections are never meshed)</li>
 *   <li>{@code indexCount > 0 && indexCount % 3 == 0} (complete triangles)</li>
 *   <li>{@code vertices.remaining() == vertexCount * 16}</li>
 *   <li>{@code indices.remaining() == indexCount * 4}</li>
 *   <li>All index values are in range {@code [0, vertexCount)}</li>
 * </ul>
 *
 * @param sectionX     section coordinate X
 * @param sectionY     section coordinate Y
 * @param sectionZ     section coordinate Z
 * @param vertexCount  number of vertices
 * @param indexCount   number of indices (triangle count × 3)
 * @param vertices     compact vertex data (read-only)
 * @param indices      uint32 index data (read-only)
 * @param biomeBlend   biome blending data (256 × 8 bytes), or null if disabled
 * @param lightData    block light data (4096 bytes), or null if disabled
 */
@ApiStatus.Preview("valenta")
public record MallaChunk(
        int sectionX, int sectionY, int sectionZ,
        int vertexCount, int indexCount,
        ByteBuffer vertices, ByteBuffer indices,
        ByteBuffer biomeBlend, ByteBuffer lightData) {

    public MallaChunk {
        if (vertexCount <= 0) {
            throw new IllegalArgumentException("vertexCount <= 0: " + vertexCount);
        }
        if (indexCount <= 0 || indexCount % 3 != 0) {
            throw new IllegalArgumentException(
                    "indexCount must be > 0 and divisible by 3: " + indexCount);
        }
        Objects.requireNonNull(vertices, "vertices");
        Objects.requireNonNull(indices, "indices");
        int expectedVBytes = vertexCount * CompactVertexFormat.BYTES_PER_VERTEX;
        if (vertices.remaining() != expectedVBytes) {
            throw new IllegalArgumentException(
                    "vertices size mismatch: " + vertices.remaining() + " != " + expectedVBytes);
        }
        int expectedIBytes = indexCount * 4;
        if (indices.remaining() != expectedIBytes) {
            throw new IllegalArgumentException(
                    "indices size mismatch: " + indices.remaining() + " != " + expectedIBytes);
        }
    }

    /**
     * Unique section key, suitable for use as a map key or section ID.
     * Packs coordinates into a long: X in bits [42..21], Z in [20..0], Y in sign+high bits.
     */
    public long sectionKey() {
        return packSectionKey(sectionX, sectionY, sectionZ);
    }

    /**
     * Packs section coordinates into a unique long key.
     */
    public static long packSectionKey(int sx, int sy, int sz) {
        return ((long) sx & 0x1FFFFFL) << 42
                | ((long) sz & 0x1FFFFFL) << 21
                | ((long) sy & 0x1FFFFFL);
    }

    /**
     * Builder for creating MallaChunk instances from meshing results.
     */
    public static final class Builder {
        private final int sectionX, sectionY, sectionZ;
        private ByteBuffer vertices;
        private ByteBuffer indices;
        private int vertexCount;
        private int indexCount;
        private ByteBuffer biomeBlend;
        private ByteBuffer lightData;

        public Builder(int sectionX, int sectionY, int sectionZ) {
            this.sectionX = sectionX;
            this.sectionY = sectionY;
            this.sectionZ = sectionZ;
            this.vertices = ByteBuffer.allocateDirect(1024 * CompactVertexFormat.BYTES_PER_VERTEX)
                    .order(ByteOrder.nativeOrder());
            this.indices = ByteBuffer.allocateDirect(1536 * 4)
                    .order(ByteOrder.nativeOrder());
        }

        /**
         * Adds a quad (two triangles, six indices, four vertices).
         */
        @ApiStatus.HotPath
        public void addQuad(float x0, float y0, float z0,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3,
                            float nx, float ny,
                            int rgba,
                            float u0, float v0, float u1, float v1) {
            ensureVertexCapacity(4);
            ensureIndexCapacity(6);

            int base = vertexCount;
            int vOff = vertexCount * CompactVertexFormat.BYTES_PER_VERTEX;

            CompactVertexFormat.encode(vertices, vOff, x0, y0, z0, nx, ny, rgba, u0, v0);
            vOff += CompactVertexFormat.BYTES_PER_VERTEX;
            CompactVertexFormat.encode(vertices, vOff, x1, y1, z1, nx, ny, rgba, u1, v0);
            vOff += CompactVertexFormat.BYTES_PER_VERTEX;
            CompactVertexFormat.encode(vertices, vOff, x2, y2, z2, nx, ny, rgba, u1, v1);
            vOff += CompactVertexFormat.BYTES_PER_VERTEX;
            CompactVertexFormat.encode(vertices, vOff, x3, y3, z3, nx, ny, rgba, u0, v1);
            vertexCount += 4;

            int iOff = indexCount * 4;
            indices.putInt(iOff, base);
            indices.putInt(iOff + 4, base + 1);
            indices.putInt(iOff + 8, base + 2);
            indices.putInt(iOff + 12, base);
            indices.putInt(iOff + 16, base + 2);
            indices.putInt(iOff + 20, base + 3);
            indexCount += 6;
        }

        public Builder biomeBlend(ByteBuffer data) {
            this.biomeBlend = data;
            return this;
        }

        public Builder lightData(ByteBuffer data) {
            this.lightData = data;
            return this;
        }

        /**
         * Builds an immutable MallaChunk. Returns null if no geometry was added.
         */
        public MallaChunk build() {
            if (vertexCount == 0) return null;
            vertices.position(0).limit(vertexCount * CompactVertexFormat.BYTES_PER_VERTEX);
            indices.position(0).limit(indexCount * 4);
            return new MallaChunk(
                    sectionX, sectionY, sectionZ,
                    vertexCount, indexCount,
                    vertices.asReadOnlyBuffer(),
                    indices.asReadOnlyBuffer(),
                    biomeBlend, lightData);
        }

        private void ensureVertexCapacity(int additional) {
            int needed = (vertexCount + additional) * CompactVertexFormat.BYTES_PER_VERTEX;
            if (needed > vertices.capacity()) {
                ByteBuffer grown = ByteBuffer.allocateDirect(
                        Math.max(vertices.capacity() * 2, needed)).order(ByteOrder.nativeOrder());
                vertices.flip();
                grown.put(vertices);
                vertices = grown;
            }
        }

        private void ensureIndexCapacity(int additional) {
            int needed = (indexCount + additional) * 4;
            if (needed > indices.capacity()) {
                ByteBuffer grown = ByteBuffer.allocateDirect(
                        Math.max(indices.capacity() * 2, needed)).order(ByteOrder.nativeOrder());
                indices.flip();
                grown.put(indices);
                indices = grown;
            }
        }
    }
}
