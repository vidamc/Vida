/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.core;

import dev.vida.core.ApiStatus;
import java.nio.ByteBuffer;

/**
 * Compact 16-byte vertex format replacing vanilla's 28-byte layout.
 *
 * <pre>
 * Offset  Size  Field
 * ──────  ────  ─────────────────────────────────
 *  0      2     posX   (short, block-relative: value / 4096.0 → [0,1))
 *  2      2     posY   (short, block-relative)
 *  4      2     posZ   (short, block-relative)
 *  6      2     normal (byte2: nx, ny packed; nz derived in shader)
 *  8      4     color  (RGBA8 packed into int)
 * 12      2     texU   (short, atlas-relative: value / 32768.0 → [0,1))
 * 14      2     texV   (short, atlas-relative)
 * ──────
 * Total: 16 bytes
 * </pre>
 *
 * <p>Compared with vanilla's 28-byte format, this reduces VRAM bandwidth
 * by ~43% and significantly improves L2 cache hit rate on the GPU.
 */
@ApiStatus.Preview("valenta")
public final class CompactVertexFormat {

    public static final int BYTES_PER_VERTEX = 16;
    public static final int POSITION_OFFSET = 0;
    public static final int NORMAL_OFFSET = 6;
    public static final int COLOR_OFFSET = 8;
    public static final int TEXCOORD_OFFSET = 12;

    private static final float POSITION_SCALE = 4096.0f;
    private static final float TEXCOORD_SCALE = 32768.0f;

    private CompactVertexFormat() {}

    /**
     * Encodes a single vertex into the target buffer at the given byte offset.
     *
     * @param buf    target buffer (capacity >= offset + 16)
     * @param offset byte offset into the buffer
     * @param x      block-relative x in [0, 16)
     * @param y      block-relative y in [0, 16)
     * @param z      block-relative z in [0, 16)
     * @param nx     normal x in [-1, 1]
     * @param ny     normal y in [-1, 1]
     * @param rgba   color as RGBA8 packed int
     * @param u      atlas-relative u in [0, 1)
     * @param v      atlas-relative v in [0, 1)
     */
    @ApiStatus.HotPath
    public static void encode(ByteBuffer buf, int offset,
                              float x, float y, float z,
                              float nx, float ny,
                              int rgba,
                              float u, float v) {
        buf.putShort(offset + POSITION_OFFSET, quantizePosition(x));
        buf.putShort(offset + POSITION_OFFSET + 2, quantizePosition(y));
        buf.putShort(offset + POSITION_OFFSET + 4, quantizePosition(z));
        buf.put(offset + NORMAL_OFFSET, quantizeNormal(nx));
        buf.put(offset + NORMAL_OFFSET + 1, quantizeNormal(ny));
        buf.putInt(offset + COLOR_OFFSET, rgba);
        buf.putShort(offset + TEXCOORD_OFFSET, quantizeTexcoord(u));
        buf.putShort(offset + TEXCOORD_OFFSET + 2, quantizeTexcoord(v));
    }

    /**
     * Decodes position X from encoded data at offset.
     *
     * @param buf    source buffer
     * @param offset byte offset to the vertex start
     * @return block-relative x
     */
    public static float decodeX(ByteBuffer buf, int offset) {
        return buf.getShort(offset + POSITION_OFFSET) / POSITION_SCALE;
    }

    public static float decodeY(ByteBuffer buf, int offset) {
        return buf.getShort(offset + POSITION_OFFSET + 2) / POSITION_SCALE;
    }

    public static float decodeZ(ByteBuffer buf, int offset) {
        return buf.getShort(offset + POSITION_OFFSET + 4) / POSITION_SCALE;
    }

    public static float decodeNx(ByteBuffer buf, int offset) {
        return buf.get(offset + NORMAL_OFFSET) / 127.0f;
    }

    public static float decodeNy(ByteBuffer buf, int offset) {
        return buf.get(offset + NORMAL_OFFSET + 1) / 127.0f;
    }

    public static int decodeColor(ByteBuffer buf, int offset) {
        return buf.getInt(offset + COLOR_OFFSET);
    }

    public static float decodeU(ByteBuffer buf, int offset) {
        return buf.getShort(offset + TEXCOORD_OFFSET) / TEXCOORD_SCALE;
    }

    public static float decodeV(ByteBuffer buf, int offset) {
        return buf.getShort(offset + TEXCOORD_OFFSET + 2) / TEXCOORD_SCALE;
    }

    static short quantizePosition(float value) {
        return (short) Math.round(value * POSITION_SCALE);
    }

    static short quantizeTexcoord(float value) {
        return (short) Math.round(value * TEXCOORD_SCALE);
    }

    static byte quantizeNormal(float value) {
        return (byte) Math.round(Math.clamp(value, -1.0f, 1.0f) * 127.0f);
    }
}
