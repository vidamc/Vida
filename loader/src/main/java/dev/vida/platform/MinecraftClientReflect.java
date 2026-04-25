/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.platform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Caché {@link MethodHandle} hacia <strong>Minecraft#getInstance</strong>,
 * <strong>getWindow</strong> y <strong>level</strong> — reutilizable entre
 * {@link VanillaBridge} e implementaciones <strong>:cima</strong>.
 */
final class MinecraftClientReflect {

    final MethodHandle minecraftGetInstance;
    final MethodHandle minecraftLevel;
    final MethodHandle minecraftGetWindow;
    final MethodHandle windowGuiScaledWidth;
    final MethodHandle windowGuiScaledHeight;
    final MethodHandle levelGetDayTime;

    private static volatile MinecraftClientReflect INSTANCE;
    private static volatile boolean initFailed;

    private MinecraftClientReflect(
            MethodHandle minecraftGetInstance,
            MethodHandle minecraftLevel,
            MethodHandle minecraftGetWindow,
            MethodHandle windowGuiScaledWidth,
            MethodHandle windowGuiScaledHeight,
            MethodHandle levelGetDayTime) {
        this.minecraftGetInstance = minecraftGetInstance;
        this.minecraftLevel = minecraftLevel;
        this.minecraftGetWindow = minecraftGetWindow;
        this.windowGuiScaledWidth = windowGuiScaledWidth;
        this.windowGuiScaledHeight = windowGuiScaledHeight;
        this.levelGetDayTime = levelGetDayTime;
    }

    static MinecraftClientReflect instance() {
        MinecraftClientReflect local = INSTANCE;
        if (local != null) {
            return local;
        }
        if (initFailed) {
            return null;
        }
        synchronized (MinecraftClientReflect.class) {
            if (INSTANCE != null) {
                return INSTANCE;
            }
            if (initFailed) {
                return null;
            }
            try {
                INSTANCE = crear();
                return INSTANCE;
            } catch (ReflectiveOperationException e) {
                initFailed = true;
                return null;
            }
        }
    }

    private static MinecraftClientReflect crear() throws ReflectiveOperationException, IllegalAccessException {
        MethodHandles.Lookup pub = MethodHandles.publicLookup();
        Class<?> mcCl = Class.forName("net.minecraft.client.Minecraft");
        Class<?> winCl = Class.forName("com.mojang.blaze3d.platform.Window");
        Class<?> lvlCl = Class.forName("net.minecraft.world.level.Level");
        Method gi = mcCl.getMethod("getInstance");
        Method lvl = mcCl.getMethod("level");
        Method gw = mcCl.getMethod("getWindow");
        Method wW = winCl.getMethod("getGuiScaledWidth");
        Method wH = winCl.getMethod("getGuiScaledHeight");
        Method gdt = lvlCl.getMethod("getDayTime");
        return new MinecraftClientReflect(
                pub.unreflect(gi),
                pub.unreflect(lvl),
                pub.unreflect(gw),
                pub.unreflect(wW),
                pub.unreflect(wH),
                pub.unreflect(gdt));
    }

    static void resetForTests() {
        synchronized (MinecraftClientReflect.class) {
            INSTANCE = null;
            initFailed = false;
        }
    }

    static Optional<Object> nivelHabitualOEmpty() {
        MinecraftClientReflect r = instance();
        if (r == null) {
            return Optional.empty();
        }
        try {
            Object mc = r.minecraftGetInstance.invoke();
            if (mc == null) {
                return Optional.empty();
            }
            Object level = r.minecraftLevel.invoke(mc);
            if (level == null) {
                return Optional.empty();
            }
            return Optional.of(level);
        } catch (RuntimeException ex) {
            return Optional.empty();
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }
}
