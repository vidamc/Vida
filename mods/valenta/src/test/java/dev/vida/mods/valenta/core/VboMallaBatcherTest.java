/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VboMallaBatcherTest {

    private GlFunctions.Noop gl;
    private VboMallaBatcher batcher;

    @BeforeEach
    void setUp() {
        gl = new GlFunctions.Noop();
        batcher = new VboMallaBatcher(gl, 256, 384);
    }

    @AfterEach
    void tearDown() {
        batcher.close();
    }

    @Test
    void beginFrame_resetsCounters() {
        batcher.beginFrame();
        assertThat(batcher.totalVertices()).isZero();
        assertThat(batcher.totalIndices()).isZero();
        assertThat(batcher.commandCount()).isZero();
    }

    @Test
    void appendSection_updatesCounters() {
        batcher.beginFrame();
        ByteBuffer verts = ByteBuffer.allocateDirect(4 * 16).order(ByteOrder.nativeOrder());
        ByteBuffer idxs = ByteBuffer.allocateDirect(6 * 4).order(ByteOrder.nativeOrder());

        batcher.appendSection(verts, idxs, 4, 6, 1);
        assertThat(batcher.totalVertices()).isEqualTo(4);
        assertThat(batcher.totalIndices()).isEqualTo(6);
        assertThat(batcher.commandCount()).isEqualTo(1);
    }

    @Test
    void multipleSections_accumulate() {
        batcher.beginFrame();
        for (int i = 0; i < 5; i++) {
            ByteBuffer v = ByteBuffer.allocateDirect(4 * 16).order(ByteOrder.nativeOrder());
            ByteBuffer idx = ByteBuffer.allocateDirect(6 * 4).order(ByteOrder.nativeOrder());
            batcher.appendSection(v, idx, 4, 6, i);
        }
        assertThat(batcher.totalVertices()).isEqualTo(20);
        assertThat(batcher.totalIndices()).isEqualTo(30);
        assertThat(batcher.commandCount()).isEqualTo(5);
    }

    @Test
    void flush_returnsCommandCount() {
        batcher.beginFrame();
        ByteBuffer v = ByteBuffer.allocateDirect(4 * 16).order(ByteOrder.nativeOrder());
        ByteBuffer idx = ByteBuffer.allocateDirect(6 * 4).order(ByteOrder.nativeOrder());
        batcher.appendSection(v, idx, 4, 6, 0);

        int flushed = batcher.flush();
        assertThat(flushed).isEqualTo(1);
    }

    @Test
    void flush_emptyFrame_returnsZero() {
        batcher.beginFrame();
        assertThat(batcher.flush()).isZero();
    }

    @Test
    void close_preventsAppend() {
        batcher.close();
        assertThatThrownBy(batcher::beginFrame)
                .isInstanceOf(IllegalStateException.class);
    }
}
