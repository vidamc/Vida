/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.platform;

import dev.vida.base.latidos.LatidoBus;
import dev.vida.core.ApiStatus;
import dev.vida.render.LatidoRenderHud;
import dev.vida.render.PintorHud;
import java.lang.invoke.MethodHandle;

/**
 * Мост между vanilla-Minecraft и шиной событий Vida.
 *
 * <p>Платформенные морфы ({@link MinecraftTickMorph}, {@link GuiRenderMorph})
 * не вызывают {@link LatidoBus} напрямую: они проксируют запрос через
 * {@code PlatformBridge}. Это даёт три преимущества:
 * <ul>
 *   <li><b>Тестируемость.</b> В unit-тестах мост подменяется на mock,
 *       без поднятия классов Minecraft и полной шины.</li>
 *   <li><b>Отсутствие compile-time зависимости от Minecraft.</b> Реальная
 *       реализация ({@link VanillaBridge}) живёт в отдельном классе и
 *       использует лёгкую привязку {@link java.lang.invoke.MethodHandle} —
 *       морфы остаются «тонкими».</li>
 *   <li><b>Единая точка подсчёта тиков и fallback-логики.</b> Если шина
 *       ещё не установлена, bridge молча игнорирует вызов: морфы не
 *       падают и не спамят логи.</li>
 * </ul>
 *
 * <h2>Contract</h2>
 * Реализация обязана быть thread-safe: {@link #onClientTick()} всегда
 * вызывается из client-thread, но {@link #onHudRender(Object, float)} может
 * быть вызван из render-thread, который у Minecraft совпадает с main thread,
 * но у других платформенных реализаций может и не совпадать.
 */
@ApiStatus.Internal
public interface PlatformBridge {

    /** Сбрасывает кэш рефлексии (только тесты). */
    @ApiStatus.Internal
    static void resetGfxFillCacheForTests() {
        GuiGraphicsFillReflect.resetForTests();
        GuiGraphicsDrawStringReflect.resetForTests();
    }

    /**
     * Вызывается из {@link MinecraftTickMorph} один раз за client-tick.
     * Публикует {@code LatidoPulso(tick, null, 0)} на глобальную шину.
     */
    void onClientTick();

    /**
     * Вызывается из {@link ServerTickMorph} в начале каждого серверного тика.
     *
     * @param servidorMinecraft {@code net.minecraft.server.MinecraftServer}
     */
    default void onServerTick(Object servidorMinecraft) {}

    /**
     * Вызывается из {@link GuiRenderMorph} в начале каждого HUD-кадра.
     * Публикует {@link LatidoRenderHud} на глобальную шину.
     *
     * @param guiGraphics {@code net.minecraft.client.gui.GuiGraphics} —
     *                    передаётся как {@link Object}, чтобы интерфейс не
     *                    тянул Minecraft в classpath
     * @param partialTick доля тика {@code [0.0, 1.0]}
     */
    void onHudRender(Object guiGraphics, float partialTick);

    /**
     * Хелпер для {@link VanillaBridge}: строит {@link PintorHud}, который
     * делегирует {@code GuiGraphics.fill} через {@link MethodHandle}. Вынесен
     * сюда, чтобы его можно было переопределить в
     * заглушках для тестов.
     *
     * @param guiGraphics живой vanilla-объект
     * @return {@code null}, если {@code guiGraphics == null}
     */
    static PintorHud pintorOver(Object guiGraphics) {
        if (guiGraphics == null) return null;
        MethodHandle fill = GuiGraphicsFillReflect.resolve(guiGraphics);
        if (fill == null) {
            return (x, y, w, h, argb) -> { };
        }
        return (x, y, w, h, argb) -> {
            try {
                fill.invoke(guiGraphics, x, y, x + w, y + h, argb);
            } catch (Throwable ignored) {
                // Любая ошибка (старая/обфусцированная версия MC) — тихо пропускаем
                // кадр для конкретного подписчика. Другие подписчики продолжат работу.
            }
        };
    }
}
