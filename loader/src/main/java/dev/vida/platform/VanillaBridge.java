/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.platform;

import dev.vida.base.LatidoGlobal;
import dev.vida.base.latidos.LatidoBus;
import dev.vida.base.latidos.eventos.LatidoPulso;
import dev.vida.core.ApiStatus;
import dev.vida.loader.internal.ClientEntrypointScheduler;
import dev.vida.core.Log;
import dev.vida.mundo.latidos.LatidosMundo;
import dev.vida.render.LatidoRenderHud;
import dev.vida.render.PintorHud;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
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
 * Чтение вьюпорта идёт через закэшированные {@link MethodHandle} (ТЗ Vida:
 * горячий путь без {@code Method.invoke}). Разрешение имён — один раз при
 * первом успешном обращении; при ошибке кадр/тик мира пропускается.
 */
@ApiStatus.Internal
public final class VanillaBridge implements PlatformBridge {

    private static final Log LOG = Log.of(VanillaBridge.class);

    /** Синглтон, устанавливаемый {@link dev.vida.loader.internal.BootSequence}. */
    private static volatile PlatformBridge INSTANCE;

    private final AtomicLong tickCounter = new AtomicLong(0);

    private final AtomicLong serverTickCounter = new AtomicLong(0);

    /** {@link PintorHud} без аллокаций на кадр: валиден только на время {@link #onHudRender}. */
    private final GraphicsBoundPintor pintorFrame = new GraphicsBoundPintor();

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
        McClientReflect.resetForTests();
        PlatformBridge.resetGfxFillCacheForTests();
        ClientEntrypointScheduler.resetForNewBootSession();
        MundoNivelVanilla.resetForTests();
        ServidorLatidoMundo.resetForTests();
    }

    // ---------------------------------------------------------- PlatformBridge

    @Override
    public void onClientTick() {
        ClientEntrypointScheduler.flushPendingOnce();
        LatidoBus bus = LatidoGlobal.maybeCurrent().orElse(null);
        if (bus == null) return;
        long tick = tickCounter.getAndIncrement();
        try {
            bus.emitir(LatidoPulso.TIPO, LatidoPulso.raiz(tick));
        } catch (RuntimeException ex) {
            LOG.warn("Vida: LatidoPulso dispatch failed ({})", ex.toString());
        }
        if (bus.cantidadSuscriptores(LatidosMundo.Tick.TIPO) > 0) {
            emitirLatidoMundoSiNivel(bus, tick);
        }
    }

    @Override
    public void onServerTick(Object servidorMinecraft) {
        LatidoBus bus = LatidoGlobal.maybeCurrent().orElse(null);
        if (bus == null) {
            return;
        }
        long tick = serverTickCounter.getAndIncrement();
        ServidorLatidoMundo.emitirSiHaySuscriptores(bus, servidorMinecraft, tick);
    }

    private static void emitirLatidoMundoSiNivel(LatidoBus bus, long tick) {
        McClientReflect r = McClientReflect.instance();
        if (r == null) {
            return;
        }
        try {
            Object mc = r.minecraftGetInstance.invoke();
            if (mc == null) {
                return;
            }
            Object level = r.minecraftLevel.invoke(mc);
            if (level == null) {
                return;
            }
            Object dayTimeObj = r.levelGetDayTime.invoke(level);
            long dayTime = dayTimeObj instanceof Number n ? n.longValue() : 0L;
            long ciclo = Math.floorMod(dayTime, 24000L);
            var mundo = new MundoNivelVanilla(level);
            bus.emitir(
                    LatidosMundo.Tick.TIPO,
                    new LatidosMundo.Tick(mundo, tick, ciclo));
        } catch (RuntimeException ex) {
            LOG.warn("Vida: LatidosMundo.Tick dispatch failed ({})", ex.toString());
        } catch (Throwable ignored) {
            // клиент без Minecraft на classpath — норма для тестов
        }
    }

    @Override
    public void onHudRender(Object guiGraphics, float partialTick) {
        LatidoBus bus = LatidoGlobal.maybeCurrent().orElse(null);
        if (bus == null) return;
        if (bus.cantidadSuscriptores(LatidoRenderHud.TIPO) <= 0) {
            return;
        }

        int w = guiScaledWidth();
        int h = guiScaledHeight();
        if (w <= 0 || h <= 0) return;

        pintorFrame.prepare(guiGraphics);
        try {
            bus.emitir(LatidoRenderHud.TIPO,
                    new LatidoRenderHud(w, h, partialTick, pintorFrame));
        } catch (RuntimeException ex) {
            LOG.warn("Vida: LatidoRenderHud dispatch failed ({})", ex.toString());
        } finally {
            pintorFrame.clear();
        }
    }

    // ----------------------------------------------------------------- helpers

    /** Для тестов: текущее значение счётчика тиков. */
    public long tickCount() { return tickCounter.get(); }

    private int guiScaledWidth() {
        McClientReflect r = McClientReflect.instance();
        if (r == null) {
            return -1;
        }
        try {
            Object mc = r.minecraftGetInstance.invoke();
            if (mc == null) return -1;
            Object window = r.minecraftGetWindow.invoke(mc);
            if (window == null) return -1;
            return (int) r.windowGuiScaledWidth.invoke(window);
        } catch (Throwable ex) {
            return -1;
        }
    }

    private int guiScaledHeight() {
        McClientReflect r = McClientReflect.instance();
        if (r == null) {
            return -1;
        }
        try {
            Object mc = r.minecraftGetInstance.invoke();
            if (mc == null) return -1;
            Object window = r.minecraftGetWindow.invoke(mc);
            if (window == null) return -1;
            return (int) r.windowGuiScaledHeight.invoke(window);
        } catch (Throwable ex) {
            return -1;
        }
    }

    // ----------------------------------------------------------------- reflection cache

    private static final class McClientReflect {
        final MethodHandle minecraftGetInstance;
        final MethodHandle minecraftLevel;
        final MethodHandle minecraftGetWindow;
        final MethodHandle windowGuiScaledWidth;
        final MethodHandle windowGuiScaledHeight;
        final MethodHandle levelGetDayTime;

        private static volatile McClientReflect INSTANCE;
        private static volatile boolean initFailed;

        private McClientReflect(
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

        static McClientReflect instance() {
            McClientReflect local = INSTANCE;
            if (local != null) {
                return local;
            }
            if (initFailed) {
                return null;
            }
            synchronized (McClientReflect.class) {
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

        private static McClientReflect crear() throws ReflectiveOperationException, IllegalAccessException {
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
            return new McClientReflect(
                    pub.unreflect(gi),
                    pub.unreflect(lvl),
                    pub.unreflect(gw),
                    pub.unreflect(wW),
                    pub.unreflect(wH),
                    pub.unreflect(gdt));
        }

        static void resetForTests() {
            synchronized (McClientReflect.class) {
                INSTANCE = null;
                initFailed = false;
            }
        }
    }

    /**
     * Один экземпляр на {@link VanillaBridge}: {@link #prepare} перед
     * {@code emitir}, {@link #clear} после — подписчики не должны удерживать
     * событие и вызывать {@code pintor} позже.
     */
    private static final class GraphicsBoundPintor implements PintorHud {
        private Object graphics;

        void prepare(Object guiGraphics) {
            this.graphics = guiGraphics;
        }

        void clear() {
            this.graphics = null;
        }

        @Override
        public void dibujarRectangulo(int x, int y, int ancho, int alto, int colorArgb) {
            Object g = graphics;
            if (g == null || ancho <= 0 || alto <= 0) {
                return;
            }
            MethodHandle fill = GuiGraphicsFillReflect.resolve(g);
            if (fill == null) {
                return;
            }
            try {
                fill.invoke(g, x, y, x + ancho, y + alto, colorArgb);
            } catch (Throwable ignored) {
                // старая/обфусцированная версия MC — пропускаем прямоугольник
            }
        }

        @Override
        public void dibujarCadena(int x, int y, String texto, int colorArgb) {
            Object g = graphics;
            if (g == null || texto == null || texto.isEmpty()) {
                return;
            }
            GuiGraphicsDrawStringReflect.dibujar(g, texto, x, y, colorArgb);
        }
    }
}
