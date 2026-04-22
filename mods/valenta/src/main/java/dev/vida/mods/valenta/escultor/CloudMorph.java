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

/**
 * Morphs {@code LevelRenderer#renderClouds} to apply cloud rendering
 * settings (vanilla / fast / disabled).
 *
 * <p>When clouds are disabled, the entire method is short-circuited.
 * When fast clouds are selected, Valenta substitutes its own flat-plane
 * renderer.
 */
@ApiStatus.Internal
@VifadaMorph(target = "net.minecraft.client.renderer.LevelRenderer", priority = 800)
public final class CloudMorph {

    @VifadaInject(
            method = "renderClouds(Lcom/mojang/blaze3d/vertex/PoseStack;Lorg/joml/Matrix4f;FDDD)V",
            at = @VifadaAt(InjectionPoint.HEAD),
            requireTarget = false)
    private void onRenderClouds(Object poseStack, Object projectionMatrix,
                                float partialTick, double camX, double camY, double camZ,
                                CallbackInfo ci) {
        if (!ValentaHooks.shouldRenderClouds()) {
            ci.cancel();
        }
    }
}
