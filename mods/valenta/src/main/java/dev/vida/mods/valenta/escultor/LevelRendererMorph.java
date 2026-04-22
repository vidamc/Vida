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
 * Morphs {@code LevelRenderer} to replace the chunk rendering pipeline
 * with Valenta's VBO-batched multi-draw indirect path.
 *
 * <p>Injected at the head of {@code renderLevel} to intercept the full
 * render pass. The shadow field {@code viewArea} provides access to the
 * chunk data structure for section iteration.
 */
@ApiStatus.Internal
@VifadaMorph(target = "net.minecraft.client.renderer.LevelRenderer", priority = 900)
public abstract class LevelRendererMorph {

    @VifadaShadow
    private Object viewArea;

    @VifadaInject(
            method = "setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V",
            at = @VifadaAt(InjectionPoint.HEAD),
            requireTarget = false)
    private void onSetupRender(Object camera, Object frustum,
                               boolean hasCapturedFrustum, boolean spectator,
                               CallbackInfo ci) {
        ValentaHooks.onSetupRender(viewArea, camera, frustum);
    }

    @VifadaInject(
            method = "renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;)V",
            at = @VifadaAt(InjectionPoint.HEAD),
            requireTarget = false)
    private void onRenderLevel(Object poseStack, float partialTick, long nanoTime,
                               boolean renderBlockOutline, Object camera,
                               Object gameRenderer, Object lightTexture,
                               Object projectionMatrix, CallbackInfo ci) {
        ValentaHooks.onRenderLevelStart(partialTick, nanoTime);
    }
}
