/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.platform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * Кэш {@code GuiGraphics.fill} для горячего HUD-пути: один раз
 * {@link Method#unreflect} → дальше {@link MethodHandle#invoke}, без
 * повторного {@code getMethod} и без {@code Method.invoke} (см. ТЗ Vida).
 */
final class GuiGraphicsFillReflect {

    private static volatile MethodHandle cached;

    private GuiGraphicsFillReflect() {}

    static MethodHandle resolve(Object guiGraphics) {
        MethodHandle mh = cached;
        if (mh != null) {
            return mh;
        }
        synchronized (GuiGraphicsFillReflect.class) {
            if (cached == null) {
                try {
                    Method m = guiGraphics.getClass().getMethod(
                            "fill", int.class, int.class, int.class, int.class, int.class);
                    cached = MethodHandles.publicLookup().unreflect(m);
                } catch (ReflectiveOperationException ignored) {
                    return null;
                }
            }
            return cached;
        }
    }

    static void resetForTests() {
        synchronized (GuiGraphicsFillReflect.class) {
            cached = null;
        }
    }
}
