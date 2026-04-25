/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cima;

import dev.vida.core.ApiStatus;
import dev.vida.mundo.Mundo;
import java.util.Optional;

/**
 * <strong>Cima</strong> — публичная «надстройка» над <strong>тем же</strong>
 * vanilla-окружением, с которым загрузчик строит
 * <strong>{@code MundoNivelVanilla}</strong> и
 * <strong>LatidosMundo.Tick</strong>. Даёт и типизированный
 * <strong>Mundo</strong> (vida-mundo), и
 * <strong>сырую</strong> ссылку <strong>{@code Object} = net.minecraft
 * .world.level.Level</strong> для сценариев, которых не хватает у
 * портABLE-абстракций (низкоуровневый чанк, шум, private API) — в связке
 * с <strong>Puertas / Vifada</strong> (см. <strong>docs/modules/cima.md</strong>).
 *
 * <p>Экземпляр: {@link CimaJuegoGlobal#cimaJuego()}. До монтирования
 * загрузчиком — {@link CimaJuegoNulo#INSTANCIA}. Не путать с
 * намеренно-абстрактным API модуля {@code :mundo}: cima — осознанный
 * escape-hatch, привязанный к Mojang, со своей волатильностью на апдейты MC.
 */
@ApiStatus.Preview("cima")
public interface CimaJuego {

    /** True, если сейчас на клиенте известен непустой {@code Minecraft#level} (например в игре, не в меню). */
    boolean vinculado();

    /** Тот же <strong>Level</strong> (as {@code Object}), что питает LatidosMundo на клиенте, если он есть. */
    Optional<Object> nivelMinecraftVivo();

    /** Эквивалент <strong>{@code new MundoNivelVanilla(nivelMinecraftVivo.get())}</strong> из <strong>:loader</strong>. */
    Optional<Mundo> mundoSobreNivelCargado();
}
