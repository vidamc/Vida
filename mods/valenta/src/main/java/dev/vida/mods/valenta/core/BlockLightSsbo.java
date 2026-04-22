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
 * SSBO for per-block light values within sections.
 *
 * <p>Each section stores 4096 light entries (16³ blocks × 1 byte each):
 * the high nibble is sky light, the low nibble is block light. The shader
 * unpacks and interpolates smooth lighting from this buffer.
 *
 * <p>Bound at a fixed binding point, indexed by
 * {@code sectionId * 4096 + (y * 256 + z * 16 + x)}.
 */
@ApiStatus.Preview("valenta")
public final class BlockLightSsbo implements AutoCloseable {

    public static final int ENTRIES_PER_SECTION = 4096;
    public static final int BYTES_PER_ENTRY = 1;
    public static final int SECTION_BYTES = ENTRIES_PER_SECTION * BYTES_PER_ENTRY;
    public static final int BINDING_POINT = 3;

    private final GpuBuffer buffer;
    private ByteBuffer staging;
    private int sectionCount;

    public BlockLightSsbo(GlFunctions gl) {
        this.buffer = new GpuBuffer(
                GpuBuffer.Target.SHADER_STORAGE_BUFFER,
                GpuBuffer.Usage.DYNAMIC_DRAW,
                Objects.requireNonNull(gl, "gl"));
        this.staging = ByteBuffer.allocateDirect(64 * SECTION_BYTES)
                .order(ByteOrder.nativeOrder());
    }

    @ApiStatus.HotPath
    public void beginFrame() {
        staging.clear();
        sectionCount = 0;
    }

    /**
     * Appends light data for one section.
     *
     * @param lightData 4096 bytes (one per block in the section)
     */
    @ApiStatus.HotPath
    public void appendSection(ByteBuffer lightData) {
        Objects.requireNonNull(lightData, "lightData");
        if (lightData.remaining() < SECTION_BYTES) {
            throw new IllegalArgumentException(
                    "lightData too small: " + lightData.remaining() + " < " + SECTION_BYTES);
        }
        ensureCapacity(1);
        int limit = lightData.limit();
        lightData.limit(lightData.position() + SECTION_BYTES);
        staging.put(lightData);
        lightData.limit(limit);
        sectionCount++;
    }

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
        int neededBytes = (sectionCount + additionalSections) * SECTION_BYTES;
        if (neededBytes > staging.capacity()) {
            int newCap = Math.max(staging.capacity() * 2, neededBytes);
            ByteBuffer grown = ByteBuffer.allocateDirect(newCap).order(ByteOrder.nativeOrder());
            staging.flip();
            grown.put(staging);
            staging = grown;
        }
    }
}
