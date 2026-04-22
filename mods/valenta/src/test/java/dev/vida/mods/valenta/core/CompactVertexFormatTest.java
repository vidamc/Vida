/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CompactVertexFormatTest {

    @Test
    void encodeDecode_roundTrip_position() {
        ByteBuffer buf = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
        CompactVertexFormat.encode(buf, 0, 4.0f, 2.0f, 6.0f, 0, 1, 0xFFFF0000, 0.5f, 0.25f);

        assertThat(CompactVertexFormat.decodeX(buf, 0)).isCloseTo(4.0f, within(0.01f));
        assertThat(CompactVertexFormat.decodeY(buf, 0)).isCloseTo(2.0f, within(0.01f));
        assertThat(CompactVertexFormat.decodeZ(buf, 0)).isCloseTo(6.0f, within(0.01f));
    }

    @Test
    void encodeDecode_roundTrip_normal() {
        ByteBuffer buf = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
        CompactVertexFormat.encode(buf, 0, 0, 0, 0, 0.5f, -0.7f, 0, 0, 0);

        assertThat(CompactVertexFormat.decodeNx(buf, 0)).isCloseTo(0.5f, within(0.02f));
        assertThat(CompactVertexFormat.decodeNy(buf, 0)).isCloseTo(-0.7f, within(0.02f));
    }

    @Test
    void encodeDecode_roundTrip_color() {
        ByteBuffer buf = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
        int rgba = 0xAABBCCDD;
        CompactVertexFormat.encode(buf, 0, 0, 0, 0, 0, 0, rgba, 0, 0);

        assertThat(CompactVertexFormat.decodeColor(buf, 0)).isEqualTo(rgba);
    }

    @Test
    void encodeDecode_roundTrip_texcoord() {
        ByteBuffer buf = ByteBuffer.allocateDirect(16).order(ByteOrder.nativeOrder());
        CompactVertexFormat.encode(buf, 0, 0, 0, 0, 0, 0, 0, 0.75f, 0.125f);

        assertThat(CompactVertexFormat.decodeU(buf, 0)).isCloseTo(0.75f, within(0.001f));
        assertThat(CompactVertexFormat.decodeV(buf, 0)).isCloseTo(0.125f, within(0.001f));
    }

    @ParameterizedTest
    @CsvSource({"0.0", "1.0", "7.5", "3.25", "0.001"})
    void quantizePosition_preservesPrecision(float value) {
        short q = CompactVertexFormat.quantizePosition(value);
        float restored = q / 4096.0f;
        assertThat(restored).isCloseTo(value, within(0.001f));
    }

    @Test
    void quantizeNormal_clamps() {
        assertThat(CompactVertexFormat.quantizeNormal(2.0f)).isEqualTo((byte) 127);
        assertThat(CompactVertexFormat.quantizeNormal(-2.0f)).isEqualTo((byte) -127);
    }

    @Test
    void bytesPerVertex_is16() {
        assertThat(CompactVertexFormat.BYTES_PER_VERTEX).isEqualTo(16);
    }

    @Test
    void multipleVertices_adjacentInBuffer() {
        ByteBuffer buf = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
        CompactVertexFormat.encode(buf, 0, 1, 2, 3, 0, 1, 0xFF000000, 0, 0);
        CompactVertexFormat.encode(buf, 16, 4, 5, 6, 1, 0, 0x00FF0000, 0.5f, 0.5f);

        assertThat(CompactVertexFormat.decodeX(buf, 0)).isCloseTo(1.0f, within(0.01f));
        assertThat(CompactVertexFormat.decodeX(buf, 16)).isCloseTo(4.0f, within(0.01f));
        assertThat(CompactVertexFormat.decodeColor(buf, 0)).isEqualTo(0xFF000000);
        assertThat(CompactVertexFormat.decodeColor(buf, 16)).isEqualTo(0x00FF0000);
    }
}
