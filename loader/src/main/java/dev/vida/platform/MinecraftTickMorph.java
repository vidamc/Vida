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

/**
 * Vifada-морф, вставляющий вызов {@link PlatformBridge#onClientTick()} в
 * начало {@code net.minecraft.client.Minecraft.tick()} — каждый клиентский
 * тик (20 tps при стандартной частоте).
 *
 * <h2>Почему это здесь, а не в отдельном моде</h2>
 * {@code LatidoPulso} — платформенное событие, на которое завязан весь
 * {@code :base} (например, {@code @OyenteDeTick}). Отсутствие пульса
 * сделало бы шину «мёртвой» без дополнительного мод-пакета, что для
 * базового UX неприемлемо. Поэтому морф живёт в самом загрузчике, и
 * {@code BootSequence} автоматически добавляет его в {@code MorphIndex}.
 *
 * <h2>Fallback</h2>
 * {@code requireTarget = false} — если целевой класс/метод не найдены
 * (сервер-сайд, обфусцированный MC, иная версия), морф молча пропускается;
 * Vida при этом остаётся функциональной (подписчики тиков просто не
 * получают событий).
 */
@ApiStatus.Internal
@VifadaMorph(target = "net.minecraft.client.Minecraft")
public final class MinecraftTickMorph {

    @VifadaInject(
            method = "tick()V",
            at = @VifadaAt(InjectionPoint.HEAD),
            requireTarget = false)
    public void onVidaClientTick(CallbackInfo ci) {
        PlatformBridge bridge = VanillaBridge.current();
        if (bridge == null) return;
        bridge.onClientTick();
    }
}
