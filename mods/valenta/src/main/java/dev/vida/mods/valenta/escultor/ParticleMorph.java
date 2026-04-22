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
 * Morphs {@code ParticleEngine} to apply Valenta's particle filter.
 *
 * <p>When particles are set to reduce or hide, the morph short-circuits
 * the {@code add} method based on the filter decision.
 */
@ApiStatus.Internal
@VifadaMorph(target = "net.minecraft.client.particle.ParticleEngine", priority = 800)
public final class ParticleMorph {

    @VifadaInject(
            method = "add(Lnet/minecraft/client/particle/Particle;)V",
            at = @VifadaAt(InjectionPoint.HEAD),
            requireTarget = false)
    private void onAddParticle(Object particle, CallbackInfo ci) {
        if (!ValentaHooks.shouldRenderParticle()) {
            ci.cancel();
        }
    }
}
