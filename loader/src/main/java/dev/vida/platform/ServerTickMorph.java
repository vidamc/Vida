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
import java.util.function.BooleanSupplier;

/**
 * Вставка в начало {@code MinecraftServer.tickServer(BooleanSupplier)} — физический сервер и
 * интегрированный singleplayer (кроме чистого клиента без сервера).
 */
@ApiStatus.Internal
@VifadaMorph(target = "net.minecraft.server.MinecraftServer")
public final class ServerTickMorph {

    @VifadaInject(
            method = "tickServer(Ljava/util/function/BooleanSupplier;)V",
            at = @VifadaAt(InjectionPoint.HEAD),
            requireTarget = false)
    public void onVidaServerTick(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        PlatformBridge bridge = VanillaBridge.current();
        if (bridge == null) {
            return;
        }
        bridge.onServerTick((Object) this);
    }
}
