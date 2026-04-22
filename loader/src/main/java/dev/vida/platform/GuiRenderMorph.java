/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.platform;

import dev.vida.core.ApiStatus;
import dev.vida.vifada.CallbackInfo;
import dev.vida.vifada.InjectionPoint;
import dev.vida.vifada.VifadaAt;
import dev.vida.vifada.VifadaInject;
import dev.vida.vifada.VifadaMorph;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Vifada-морф, инжектирующийся в
 * {@code net.minecraft.client.gui.Gui.render(GuiGraphics, float)} и
 * делегирующий диспатч {@link dev.vida.render.LatidoRenderHud} в
 * {@link PlatformBridge}.
 *
 * <h2>Minecraft-версия</h2>
 * Mojang-mapped сигнатура для 1.20.5+. Для прошлых версий
 * {@code (Lnet/minecraft/client/gui/GuiGraphics;F)V} отсутствует, и морф
 * молча пропускается благодаря {@code requireTarget = false}.
 *
 * <h2>Compile-time зависимость от {@code GuiGraphics}</h2>
 * {@link GuiGraphics} объявлен как стаб в sourceSet {@code mcStubs}
 * модуля {@code :loader} (см. {@code loader/build.gradle.kts}). В
 * runtime-classpath агента этот стаб отсутствует — реальный класс
 * приходит из Minecraft.
 */
@ApiStatus.Internal
@VifadaMorph(target = "net.minecraft.client.gui.Gui")
public final class GuiRenderMorph {

    @VifadaInject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;F)V",
            at = @VifadaAt(InjectionPoint.HEAD),
            requireTarget = false)
    public void onVidaHudRender(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        PlatformBridge bridge = VanillaBridge.current();
        if (bridge == null) return;
        bridge.onHudRender(guiGraphics, partialTick);
    }
}
