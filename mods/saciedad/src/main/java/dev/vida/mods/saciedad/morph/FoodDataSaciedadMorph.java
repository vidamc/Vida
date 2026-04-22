/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.saciedad.morph;

import dev.vida.core.ApiStatus;
import dev.vida.mods.saciedad.SaciedadCache;
import dev.vida.vifada.CallbackInfo;
import dev.vida.vifada.InjectionPoint;
import dev.vida.vifada.VifadaAt;
import dev.vida.vifada.VifadaInject;
import dev.vida.vifada.VifadaMorph;
import dev.vida.vifada.VifadaShadow;

/**
 * Vifada-морф для {@code net.minecraft.world.food.FoodData}.
 *
 * <p>После применения загрузчиком этот класс добавляет в {@code FoodData}
 * инжектор, который при каждом вызове {@code FoodData#tick(Player)} обновляет
 * {@link SaciedadCache} текущим значением {@code saturation}.
 *
 * <h2>Access-widener</h2>
 * {@code saciedad.ptr} (рядом с {@code build.gradle.kts}) объявляет:
 * <pre>
 *   mutable field net/minecraft/world/food/FoodData saturation F
 * </pre>
 * Это позволяет читать поле без нарушения доступа.
 *
 * <h2>Жизненный цикл</h2>
 * Класс помечен {@code @ApiStatus.Internal} — он не является публичным API
 * мода. {@code vida.mod.json} перечисляет его в секции {@code "vifada"}, и
 * загрузчик регистрирует морф автоматически.
 */
@ApiStatus.Internal
@VifadaMorph(target = "net.minecraft.world.food.FoodData", priority = 500)
public abstract class FoodDataSaciedadMorph {

    /**
     * Теневая ссылка на поле {@code FoodData#saturation}.
     *
     * <p>Компилируется как обычное поле типа {@code float}; трансформер
     * Vifada перенаправляет все обращения к нему на поле реального класса.
     */
    @VifadaShadow
    private float saturation;

    /**
     * Инжект в конец метода {@code FoodData#tick(Player)}.
     *
     * <p>Вызывается vanilla каждый серверный тик, пока игрок онлайн.
     * Обновляет {@link SaciedadCache} без аллокаций.
     *
     * @param ci контекст обратного вызова
     */
    @VifadaInject(
            method = "tick(Lnet/minecraft/world/entity/player/Player;)V",
            at = @VifadaAt(InjectionPoint.RETURN))
    private void onTick(CallbackInfo ci) {
        SaciedadCache.actualizar(saturation);
    }
}
