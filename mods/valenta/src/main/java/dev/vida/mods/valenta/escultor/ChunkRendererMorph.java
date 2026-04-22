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
 * Morphs {@code SectionRenderDispatcher} to redirect chunk build tasks
 * into Valenta's {@link dev.vida.mods.valenta.chunk.ChunkTaskGraph}.
 *
 * <p>The vanilla dispatcher's {@code runTask()} is intercepted at HEAD;
 * if Valenta is handling chunk builds, the original is skipped.
 */
@ApiStatus.Internal
@VifadaMorph(target = "net.minecraft.client.renderer.chunk.SectionRenderDispatcher", priority = 900)
public final class ChunkRendererMorph {

    @VifadaInject(
            method = "runTask()Z",
            at = @VifadaAt(InjectionPoint.HEAD),
            requireTarget = false)
    private void onRunTask(CallbackInfo ci) {
        if (ValentaHooks.isChunkPipelineActive()) {
            ValentaHooks.onChunkDispatcherTick();
        }
    }
}
