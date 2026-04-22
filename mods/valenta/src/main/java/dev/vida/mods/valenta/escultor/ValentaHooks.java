/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.escultor;

import dev.vida.core.ApiStatus;

/**
 * Static hooks called from Vifada morphs. Delegates to the live
 * {@link dev.vida.mods.valenta.ValentaMod} instance.
 *
 * <p>All methods are designed for hot-path invocation: no allocations,
 * no locks, early null-checks. If Valenta is not yet initialized
 * (before {@code iniciar()}), all hooks are no-ops.
 */
@ApiStatus.Internal
public final class ValentaHooks {

    private static volatile ValentaHookReceiver receiver;

    private ValentaHooks() {}

    /**
     * Installs the hook receiver. Called once from {@code ValentaMod.iniciar()}.
     */
    public static void install(ValentaHookReceiver r) {
        receiver = r;
    }

    /**
     * Removes the hook receiver. Called from {@code ValentaMod.detener()}.
     */
    public static void uninstall() {
        receiver = null;
    }

    @ApiStatus.HotPath
    static void onSetupRender(Object viewArea, Object camera, Object frustum) {
        ValentaHookReceiver r = receiver;
        if (r != null) r.onSetupRender(viewArea, camera, frustum);
    }

    @ApiStatus.HotPath
    static void onRenderLevelStart(float partialTick, long nanoTime) {
        ValentaHookReceiver r = receiver;
        if (r != null) r.onRenderLevelStart(partialTick, nanoTime);
    }

    @ApiStatus.HotPath
    static void onGameRendererStart(float partialTick, long nanoTime) {
        ValentaHookReceiver r = receiver;
        if (r != null) r.onGameRendererStart(partialTick, nanoTime);
    }

    @ApiStatus.HotPath
    static Float getEffectiveRenderDistance() {
        ValentaHookReceiver r = receiver;
        return (r != null) ? r.getEffectiveRenderDistance() : null;
    }

    @ApiStatus.HotPath
    static boolean isChunkPipelineActive() {
        ValentaHookReceiver r = receiver;
        return r != null && r.isChunkPipelineActive();
    }

    @ApiStatus.HotPath
    static void onChunkDispatcherTick() {
        ValentaHookReceiver r = receiver;
        if (r != null) r.onChunkDispatcherTick();
    }

    @ApiStatus.HotPath
    static boolean shouldRenderParticle() {
        ValentaHookReceiver r = receiver;
        return r == null || r.shouldRenderParticle();
    }

    @ApiStatus.HotPath
    static boolean shouldRenderClouds() {
        ValentaHookReceiver r = receiver;
        return r == null || r.shouldRenderClouds();
    }

    /**
     * Interface for the mod entrypoint to receive morph callbacks.
     */
    public interface ValentaHookReceiver {
        void onSetupRender(Object viewArea, Object camera, Object frustum);
        void onRenderLevelStart(float partialTick, long nanoTime);
        void onGameRendererStart(float partialTick, long nanoTime);
        Float getEffectiveRenderDistance();
        boolean isChunkPipelineActive();
        void onChunkDispatcherTick();
        boolean shouldRenderParticle();
        boolean shouldRenderClouds();
    }
}
