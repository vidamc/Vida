/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.chunk;

import dev.vida.core.ApiStatus;
import dev.vida.mods.valenta.chunk.AnalisisEtapa.SectionRequest;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * Build stage: generates mesh geometry for a single section on a worker thread.
 *
 * <p>Takes a {@link SectionRequest} and a block data snapshot, produces a
 * {@link MallaChunk} with compact vertex/index data. The block snapshot
 * interface ({@link SectionSnapshot}) allows the caller to provide either
 * a live section view or a frozen copy.
 *
 * <h2>Threading</h2>
 * Each invocation of {@link #construir} is stateless; multiple instances
 * can run concurrently on different sections without synchronization.
 */
@ApiStatus.Preview("valenta")
public final class BuildEtapa {

    /**
     * Read-only snapshot of a 16×16×16 section's block data.
     * Implementations must be safe for off-thread reads.
     */
    public interface SectionSnapshot {
        /**
         * @return block state ID at local coordinates (x, y, z) each in [0, 15]
         */
        int blockStateAt(int x, int y, int z);

        /**
         * @return true if the block at (x, y, z) is a full opaque cube
         */
        boolean isOpaque(int x, int y, int z);

        /**
         * @return biome blend color at (x, z) packed as RGBA, or 0 if no blend
         */
        int biomeColor(int x, int z);

        /**
         * @return packed light (sky&lt;&lt;4 | block) at local coords
         */
        int lightAt(int x, int y, int z);
    }

    private final boolean biomeBlending;
    private final boolean blockLightSsbo;

    public BuildEtapa(boolean biomeBlending, boolean blockLightSsbo) {
        this.biomeBlending = biomeBlending;
        this.blockLightSsbo = blockLightSsbo;
    }

    /**
     * Builds mesh for one section.
     *
     * @param request  the section request from analysis stage
     * @param snapshot block data for the section
     * @return completed mesh, or null if section contains no visible geometry
     */
    public MallaChunk construir(SectionRequest request, SectionSnapshot snapshot) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(snapshot, "snapshot");

        MallaChunk.Builder builder = new MallaChunk.Builder(
                request.sectionX(), request.sectionY(), request.sectionZ());

        meshSection(builder, snapshot);

        if (biomeBlending) {
            builder.biomeBlend(buildBiomeBlendData(snapshot));
        }
        if (blockLightSsbo) {
            builder.lightData(buildLightData(snapshot));
        }

        return builder.build();
    }

    /**
     * Greedy meshing: iterates all blocks, culls hidden faces against neighbors,
     * emits quads for visible faces.
     */
    private void meshSection(MallaChunk.Builder builder, SectionSnapshot snapshot) {
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int state = snapshot.blockStateAt(x, y, z);
                    if (state == 0) continue;

                    int color = 0xFFFFFFFF;
                    float u0 = 0.0f, v0 = 0.0f, u1 = 1.0f / 16.0f, v1 = 1.0f / 16.0f;

                    if (!isNeighborOpaque(snapshot, x, y + 1, z)) {
                        builder.addQuad(
                                x, y + 1, z,  x + 1, y + 1, z,
                                x + 1, y + 1, z + 1,  x, y + 1, z + 1,
                                0, 1, color, u0, v0, u1, v1);
                    }
                    if (!isNeighborOpaque(snapshot, x, y - 1, z)) {
                        builder.addQuad(
                                x, y, z + 1,  x + 1, y, z + 1,
                                x + 1, y, z,  x, y, z,
                                0, -1, color, u0, v0, u1, v1);
                    }
                    if (!isNeighborOpaque(snapshot, x, y, z + 1)) {
                        builder.addQuad(
                                x, y, z + 1,  x, y + 1, z + 1,
                                x + 1, y + 1, z + 1,  x + 1, y, z + 1,
                                0, 0, color, u0, v0, u1, v1);
                    }
                    if (!isNeighborOpaque(snapshot, x, y, z - 1)) {
                        builder.addQuad(
                                x + 1, y, z,  x + 1, y + 1, z,
                                x, y + 1, z,  x, y, z,
                                0, 0, color, u0, v0, u1, v1);
                    }
                    if (!isNeighborOpaque(snapshot, x + 1, y, z)) {
                        builder.addQuad(
                                x + 1, y, z,  x + 1, y, z + 1,
                                x + 1, y + 1, z + 1,  x + 1, y + 1, z,
                                1, 0, color, u0, v0, u1, v1);
                    }
                    if (!isNeighborOpaque(snapshot, x - 1, y, z)) {
                        builder.addQuad(
                                x, y, z + 1,  x, y, z,
                                x, y + 1, z,  x, y + 1, z + 1,
                                -1, 0, color, u0, v0, u1, v1);
                    }
                }
            }
        }
    }

    private static boolean isNeighborOpaque(SectionSnapshot snap, int x, int y, int z) {
        if (x < 0 || x > 15 || y < 0 || y > 15 || z < 0 || z > 15) {
            return false;
        }
        return snap.isOpaque(x, y, z);
    }

    private ByteBuffer buildBiomeBlendData(SectionSnapshot snapshot) {
        ByteBuffer buf = ByteBuffer.allocateDirect(256 * 8).order(ByteOrder.nativeOrder());
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int color = snapshot.biomeColor(x, z);
                buf.putInt(color);
                buf.putInt(0);
            }
        }
        buf.flip();
        return buf;
    }

    private ByteBuffer buildLightData(SectionSnapshot snapshot) {
        ByteBuffer buf = ByteBuffer.allocateDirect(4096).order(ByteOrder.nativeOrder());
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    buf.put((byte) snapshot.lightAt(x, y, z));
                }
            }
        }
        buf.flip();
        return buf;
    }
}
