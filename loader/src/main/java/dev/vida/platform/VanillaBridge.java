/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.platform;

import dev.vida.base.LatidoGlobal;
import dev.vida.base.latidos.LatidoBus;
import dev.vida.base.latidos.eventos.LatidoPulso;
import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import dev.vida.render.LatidoRenderHud;
import dev.vida.render.PintorHud;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Дефолтная реализация {@link PlatformBridge} поверх Minecraft client-JAR.
 *
 * <h2>Счётчик тиков</h2>
 * Vanilla не предоставляет легкодоступного монотонного счётчика client-tick.
 * Мы ведём его сами: {@link AtomicLong} инкрементируется из
 * {@link MinecraftTickMorph} в начале каждого тика. Счётчик не обнуляется при
 * заходе/выходе из мира — это интегральный счётчик от premain до завершения
 * процесса, в духе {@code System.nanoTime}.
 *
 * <h2>Размеры экрана</h2>
 * {@link #resolveGuiDim} читает {@code Minecraft.getInstance().getWindow()} —
 * обфусцированный маппинг тот же, но метод публичен в Mojang-mapped JAR.
 * При любой reflection-ошибке возвращается {@code -1}, и HUD-кадр пропускается.
 */
@ApiStatus.Internal
public final class VanillaBridge implements PlatformBridge {

    private static final Log LOG = Log.of(VanillaBridge.class);

    /** Синглтон, устанавливаемый {@link dev.vida.loader.internal.BootSequence}. */
    private static volatile PlatformBridge INSTANCE;

    private final AtomicLong tickCounter = new AtomicLong(0);

    /** Публичный конструктор: нужен только {@code BootSequence}-у. */
    public VanillaBridge() {}

    // ----------------------------------------------------------------- API

    /** Возвращает текущий зарегистрированный bridge или {@code null}. */
    public static PlatformBridge current() {
        return INSTANCE;
    }

    /** Устанавливает bridge (вызывается из загрузчика). */
    @ApiStatus.Internal
    public static synchronized void install(PlatformBridge bridge) {
        INSTANCE = bridge;
    }

    /** Сбрасывает bridge (только для тестов). */
    @ApiStatus.Internal
    public static synchronized void resetForTests() {
        INSTANCE = null;
    }

    // ---------------------------------------------------------- PlatformBridge

    @Override
    public void onClientTick() {
        LatidoBus bus = LatidoGlobal.maybeCurrent().orElse(null);
        if (bus == null) return;
        long tick = tickCounter.getAndIncrement();
        try {
            bus.emitir(LatidoPulso.TIPO, LatidoPulso.raiz(tick));
        } catch (RuntimeException ex) {
            LOG.warn("Vida: LatidoPulso dispatch failed ({})", ex.toString());
        }
    }

    @Override
    public void onHudRender(Object guiGraphics, float partialTick) {
        LatidoBus bus = LatidoGlobal.maybeCurrent().orElse(null);
        if (bus == null) return;

        int w = resolveGuiDim("getGuiScaledWidth");
        int h = resolveGuiDim("getGuiScaledHeight");
        if (w <= 0 || h <= 0) return;

        PintorHud pintor = PlatformBridge.pintorOver(guiGraphics);
        if (pintor == null) return;

        try {
            bus.emitir(LatidoRenderHud.TIPO,
                    new LatidoRenderHud(w, h, partialTick, pintor));
        } catch (RuntimeException ex) {
            LOG.warn("Vida: LatidoRenderHud dispatch failed ({})", ex.toString());
        }
    }

    // ----------------------------------------------------------------- helpers

    /** Для тестов: текущее значение счётчика тиков. */
    public long tickCount() { return tickCounter.get(); }

    /**
     * Возвращает размер GUI-вьюпорта через {@code Minecraft.getInstance()
     * .getWindow()}. При любой ошибке — {@code -1}, кадр пропускается.
     */
    private static int resolveGuiDim(String methodName) {
        try {
            Class<?> mc = Class.forName("net.minecraft.client.Minecraft");
            Object inst = mc.getMethod("getInstance").invoke(null);
            if (inst == null) return -1;
            Object window = inst.getClass().getMethod("getWindow").invoke(inst);
            if (window == null) return -1;
            return (int) window.getClass().getMethod(methodName).invoke(window);
        } catch (ReflectiveOperationException | ClassCastException ex) {
            return -1;
        }
    }
}
