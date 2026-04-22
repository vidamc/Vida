/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.escultor;

import dev.vida.core.ApiStatus;
import dev.vida.vifada.CallbackInfo;
import dev.vida.vifada.InjectionPoint;
import dev.vida.vifada.VifadaAt;
import dev.vida.vifada.VifadaInject;
import dev.vida.vifada.VifadaMorph;
import dev.vida.vifada.VifadaShadow;

/**
 * Morphs {@code GameRenderer} to integrate Valenta's render distance
 * management and sky optimization.
 *
 * <p>The shadow field {@code renderDistance} allows Valenta to override
 * the effective render distance smoothly via
 * {@link dev.vida.mods.valenta.quality.RenderDistanceManager}.
 */
@ApiStatus.Internal
@VifadaMorph(target = "net.minecraft.client.renderer.GameRenderer", priority = 900)
public abstract class GameRendererMorph {

    @VifadaShadow
    private float renderDistance;

    @VifadaInject(
            method = "renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @VifadaAt(InjectionPoint.HEAD),
            requireTarget = false)
    private void onRenderLevel(float partialTick, long nanoTime, Object poseStack,
                               CallbackInfo ci) {
        Float override = ValentaHooks.getEffectiveRenderDistance();
        if (override != null) {
            renderDistance = override;
        }
        ValentaHooks.onGameRendererStart(partialTick, nanoTime);
    }
}
