/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Мост Vida ↔ Minecraft: классы, которые встраиваются в ключевые vanilla-методы
 * через Vifada и публикуют платформенные события на глобальную
 * {@link dev.vida.base.LatidoGlobal шину}.
 *
 * <h2>Состав</h2>
 * <ul>
 *   <li>{@link dev.vida.platform.PlatformBridge} — интерфейс моста.</li>
 *   <li>{@link dev.vida.platform.VanillaBridge} — дефолтная реализация: счётчик
 *       тиков, экран-резолвер через reflection и публикация
 *       {@link dev.vida.base.latidos.eventos.LatidoPulso} /
 *       {@link dev.vida.render.LatidoRenderHud}.</li>
 *   <li>{@link dev.vida.platform.MinecraftTickMorph} — {@code @VifadaMorph} на
 *       {@code net.minecraft.client.Minecraft#tick()V}; рассылает
 *       {@code LatidoPulso} каждый кадр клиента.</li>
 *   <li>{@link dev.vida.platform.GuiRenderMorph} — {@code @VifadaMorph} на
 *       {@code net.minecraft.client.gui.Gui#render(GuiGraphics,float)V};
 *       рассылает {@code LatidoRenderHud} каждый HUD-кадр.</li>
 * </ul>
 *
 * <h2>Как это попадает в classpath цели</h2>
 * Платформенные морфы скомпилированы в обычные классы внутри jar загрузчика.
 * {@link dev.vida.loader.internal.BootSequence} читает их байты из classpath и
 * добавляет в {@link dev.vida.loader.MorphIndex}, поэтому на целевых классах
 * Minecraft они применяются ровно так же, как и морфы из пользовательских
 * модов, — через {@code VidaClassTransformer}.
 */
@dev.vida.core.ApiStatus.Internal
package dev.vida.platform;
