/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.platform;

import dev.vida.base.LatidoGlobal;
import dev.vida.base.latidos.eventos.LatidoConfiguracionRecargada;
import dev.vida.base.latidos.eventos.LatidoFuenteRecargada;
import dev.vida.base.latidos.eventos.OrigenRecargaAjustes;
import dev.vida.core.ApiStatus;
import dev.vida.vifada.CallbackInfo;
import dev.vida.vifada.InjectionPoint;
import dev.vida.vifada.VifadaAt;
import dev.vida.vifada.VifadaInject;
import dev.vida.vifada.VifadaMorph;
import java.time.Instant;

/**
 * После перезагрузки ресурсов клиента (F3+T) публикует {@link LatidoConfiguracionRecargada} и
 * {@link LatidoFuenteRecargada}.
 */
@ApiStatus.Internal
@VifadaMorph(target = "net.minecraft.client.Minecraft")
public final class ClientResourceReloadMorph {

    @VifadaInject(
            method = "reloadResourcePacks(Z)Ljava/util/concurrent/CompletableFuture;",
            at = @VifadaAt(InjectionPoint.HEAD),
            requireTarget = false)
    public void onVidaResourceReload(boolean bl, CallbackInfo ci) {
        LatidoGlobal.maybeCurrent()
                .ifPresent(bus -> {
                    Instant t = Instant.now();
                    try {
                        bus.emitir(
                                LatidoConfiguracionRecargada.TIPO,
                                new LatidoConfiguracionRecargada(t, OrigenRecargaAjustes.RECURSOS));
                        bus.emitir(
                                LatidoFuenteRecargada.TIPO,
                                new LatidoFuenteRecargada(t, OrigenRecargaAjustes.RECURSOS));
                    } catch (RuntimeException ignored) {
                        // не ломаем перезагрузку ресурсов игры
                    }
                });
    }
}
