/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndirectDrawBufferTest {

    private IndirectDrawBuffer buf;

    @BeforeEach
    void setUp() {
        buf = new IndirectDrawBuffer();
    }

    @Test
    void empty_afterCreation() {
        assertThat(buf.commandCount()).isZero();
    }

    @Test
    void addCommand_incrementsCount() {
        buf.addCommand(36, 0, 0, 0);
        assertThat(buf.commandCount()).isEqualTo(1);
    }

    @Test
    void addCommand_dataLayout() {
        buf.addCommand(36, 100, 200, 42);
        ByteBuffer bb = buf.asByteBuffer();

        assertThat(bb.remaining()).isEqualTo(IndirectDrawBuffer.COMMAND_STRIDE);
        assertThat(bb.getInt()).isEqualTo(36);    // count
        assertThat(bb.getInt()).isEqualTo(1);     // instanceCount
        assertThat(bb.getInt()).isEqualTo(100);   // firstIndex
        assertThat(bb.getInt()).isEqualTo(200);   // baseVertex
        assertThat(bb.getInt()).isEqualTo(42);    // baseInstance
    }

    @Test
    void reset_clearsCommands() {
        buf.addCommand(6, 0, 0, 0);
        buf.addCommand(12, 6, 4, 1);
        buf.reset();
        assertThat(buf.commandCount()).isZero();
    }

    @Test
    void multipleCommands_sequential() {
        buf.addCommand(6, 0, 0, 0);
        buf.addCommand(12, 6, 4, 1);
        buf.addCommand(36, 18, 12, 2);

        assertThat(buf.commandCount()).isEqualTo(3);
        ByteBuffer bb = buf.asByteBuffer();
        assertThat(bb.remaining()).isEqualTo(3 * IndirectDrawBuffer.COMMAND_STRIDE);
    }

    @Test
    void growsAutomatically() {
        for (int i = 0; i < 1000; i++) {
            buf.addCommand(6, i * 6, i * 4, i);
        }
        assertThat(buf.commandCount()).isEqualTo(1000);
        assertThat(buf.capacity()).isGreaterThanOrEqualTo(1000);
    }
}
