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
 * SSBO (Shader Storage Buffer Object) for biome blending data.
 *
 * <p>Stores per-section biome color weights that the fragment shader uses
 * to smoothly blend between neighboring biomes. Each entry is a packed
 * {@code vec4} (RGBA16F) covering a 4x4 column of blocks, yielding
 * 16 entries per horizontal section slice and 16×16 = 256 entries per
 * 16×16×16 section.
 *
 * <p>The SSBO is bound at a fixed binding point and indexed in the shader
 * by {@code sectionId * 256 + localIndex}.
 */
@ApiStatus.Preview("valenta")
public final class BiomeBlendSsbo implements AutoCloseable {

    public static final int ENTRIES_PER_SECTION = 256;
    public static final int BYTES_PER_ENTRY = 8;
    public static final int BINDING_POINT = 2;

    private final GpuBuffer buffer;
    private ByteBuffer staging;
    private int sectionCount;

    public BiomeBlendSsbo(GlFunctions gl) {
        this.buffer = new GpuBuffer(
                GpuBuffer.Target.SHADER_STORAGE_BUFFER,
                GpuBuffer.Usage.DYNAMIC_DRAW,
                Objects.requireNonNull(gl, "gl"));
        this.staging = ByteBuffer.allocateDirect(64 * ENTRIES_PER_SECTION * BYTES_PER_ENTRY)
                .order(ByteOrder.nativeOrder());
    }

    /**
     * Resets for a new frame.
     */
    @ApiStatus.HotPath
    public void beginFrame() {
        staging.clear();
        sectionCount = 0;
    }

    /**
     * Appends biome blend data for one section.
     *
     * @param biomeData 256 entries × 8 bytes = 2048 bytes of RGBA16F data
     */
    @ApiStatus.HotPath
    public void appendSection(ByteBuffer biomeData) {
        Objects.requireNonNull(biomeData, "biomeData");
        int needed = ENTRIES_PER_SECTION * BYTES_PER_ENTRY;
        if (biomeData.remaining() < needed) {
            throw new IllegalArgumentException(
                    "biomeData too small: " + biomeData.remaining() + " < " + needed);
        }
        ensureCapacity(1);
        int limit = biomeData.limit();
        biomeData.limit(biomeData.position() + needed);
        staging.put(biomeData);
        biomeData.limit(limit);
        sectionCount++;
    }

    /**
     * Uploads staged data to GPU.
     */
    @ApiStatus.HotPath
    public void upload() {
        if (sectionCount == 0) return;
        staging.flip();
        buffer.upload(staging);
    }

    public int sectionCount() { return sectionCount; }

    public int bufferId() { return buffer.id(); }

    @Override
    public void close() {
        buffer.close();
    }

    private void ensureCapacity(int additionalSections) {
        int neededBytes = (sectionCount + additionalSections) * ENTRIES_PER_SECTION * BYTES_PER_ENTRY;
        if (neededBytes > staging.capacity()) {
            int newCap = Math.max(staging.capacity() * 2, neededBytes);
            ByteBuffer grown = ByteBuffer.allocateDirect(newCap).order(ByteOrder.nativeOrder());
            staging.flip();
            grown.put(staging);
            staging = grown;
        }
    }
}
