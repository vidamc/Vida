package com.mojang.blaze3d.platform;

import java.nio.ByteBuffer;

public class GlStateManager {
    public static void _glBindBuffer(int target, int buffer) {}
    public static void _glBufferData(int target, ByteBuffer data, int usage) {}
    public static int _glGenBuffers() { return 0; }
    public static void _glDeleteBuffers(int buffer) {}
}
