/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.render;

import dev.vida.base.latidos.Latido;
import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Событие HUD-рендеринга. Испускается один раз за кадр перед фактическим
 * выводом HUD-элементов на экран.
 *
 * <p>Подписчики должны подписываться с {@link dev.vida.base.latidos.Fase#DESPUES},
 * чтобы отрисовываться поверх стандартного HUD игры.
 *
 * <h2>Пример</h2>
 * <pre>{@code
 * bus.suscribir(
 *     LatidoRenderHud.TIPO,
 *     Prioridad.NORMAL,
 *     Fase.DESPUES,
 *     evento -> miHudRenderer.renderizar(evento)
 * );
 * }</pre>
 *
 * <h2>Гарантии</h2>
 * <ul>
 *   <li>{@code anchoPantalla} и {@code altoPantalla} всегда {@code > 0}.</li>
 *   <li>{@code deltaTick} — дробный прогресс между тиками, в диапазоне {@code [0.0, 1.0]}.</li>
 *   <li>{@link #pintor()} никогда не {@code null}; безопасно вызывать только
 *       в потоке диспатча.</li>
 * </ul>
 *
 * @param anchoPantalla ширина экрана в пикселях
 * @param altoPantalla  высота экрана в пикселях
 * @param deltaTick     дробный прогресс между двумя серверными тиками
 * @param pintor        абстракция рисования прямоугольников
 */
@ApiStatus.Stable
public record LatidoRenderHud(
        int anchoPantalla,
        int altoPantalla,
        float deltaTick,
        PintorHud pintor) {

    public LatidoRenderHud {
        if (anchoPantalla <= 0) throw new IllegalArgumentException("anchoPantalla <= 0");
        if (altoPantalla  <= 0) throw new IllegalArgumentException("altoPantalla <= 0");
        Objects.requireNonNull(pintor, "pintor");
    }

    /** Ключ канала. */
    public static final Latido<LatidoRenderHud> TIPO =
            Latido.de("vida:render_hud", LatidoRenderHud.class);
}
