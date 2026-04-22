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

class GpuBufferTest {

    private GlFunctions.Noop gl;
    private GpuBuffer buffer;

    @BeforeEach
    void setUp() {
        gl = new GlFunctions.Noop();
        buffer = new GpuBuffer(GpuBuffer.Target.ARRAY_BUFFER, GpuBuffer.Usage.DYNAMIC_DRAW, gl);
    }

    @AfterEach
    void tearDown() {
        if (!buffer.isClosed()) buffer.close();
    }

    @Test
    void newBuffer_hasValidId() {
        assertThat(buffer.id()).isGreaterThan(0);
    }

    @Test
    void upload_setsCapacity() {
        ByteBuffer data = ByteBuffer.allocateDirect(100).order(ByteOrder.nativeOrder());
        buffer.upload(data);
        assertThat(buffer.capacityBytes()).isGreaterThanOrEqualTo(100);
    }

    @Test
    void upload_growsOnLargerData() {
        ByteBuffer small = ByteBuffer.allocateDirect(50).order(ByteOrder.nativeOrder());
        buffer.upload(small);
        long cap1 = buffer.capacityBytes();

        ByteBuffer large = ByteBuffer.allocateDirect(8192).order(ByteOrder.nativeOrder());
        buffer.upload(large);
        assertThat(buffer.capacityBytes()).isGreaterThan(cap1);
    }

    @Test
    void close_setsClosedFlag() {
        buffer.close();
        assertThat(buffer.isClosed()).isTrue();
    }

    @Test
    void close_idempotent() {
        buffer.close();
        buffer.close();
        assertThat(buffer.isClosed()).isTrue();
    }

    @Test
    void upload_afterClose_throws() {
        buffer.close();
        ByteBuffer data = ByteBuffer.allocateDirect(10).order(ByteOrder.nativeOrder());
        assertThatThrownBy(() -> buffer.upload(data))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void alignUp_powerOfTwo() {
        assertThat(GpuBuffer.alignUp(1)).isEqualTo(4096);
        assertThat(GpuBuffer.alignUp(4096)).isEqualTo(4096);
        assertThat(GpuBuffer.alignUp(4097)).isEqualTo(8192);
        assertThat(GpuBuffer.alignUp(10000)).isEqualTo(16384);
    }

    @Test
    void target_preserved() {
        assertThat(buffer.target()).isEqualTo(GpuBuffer.Target.ARRAY_BUFFER);
    }
}
