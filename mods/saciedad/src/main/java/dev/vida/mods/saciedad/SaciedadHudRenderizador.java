/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.saciedad;

import dev.vida.core.ApiStatus;
import dev.vida.render.LatidoRenderHud;
import dev.vida.render.PintorHud;

/**
 * Рендерер шкалы насыщения на HUD.
 *
 * <h2>Производительность</h2>
 * После прогрева (инициализации) не выполняет ни одной аллокации на кадр:
 * все вычисления производятся с примитивными {@code int}/{@code float},
 * позиции пересчитываются только при изменении разрешения экрана.
 *
 * <h2>Размещение</h2>
 * Шкала голода в vanilla MC находится на {@code y = экранВысота − 39}.
 * Saciedad рисует шкалу насыщения относительно неё:
 * <ul>
 *   <li>{@code ARRIBA} — 10 px выше;</li>
 *   <li>{@code ABAJO} — 10 px ниже;</li>
 *   <li>{@code ENCIMA} — точно поверх.</li>
 * </ul>
 * Горизонтально: от {@code экранШирина/2 − 91} шириной 81 px (как шкала голода).
 */
@ApiStatus.Internal
public final class SaciedadHudRenderizador {

    /** Полупрозрачный чёрный фон (α = 50%). */
    private static final int COLOR_FONDO = 0x80000000;

    /** Высота шкалы в пикселях. */
    private static final int ALTO_BARRA = 5;

    /** Полная ширина шкалы при максимальном насыщении (81 px = 9 iconos × 9 px). */
    private static final int ANCHO_MAX = 81;

    /** Смещение шкалы голода от нижнего края экрана. */
    private static final int OFFSET_HAMBRE_DESDE_ABAJO = 39;

    /** Смещение соседних позиций. */
    private static final int OFFSET_VER = 10;

    private final SaciedadConfig config;

    /** Кэшированные значения, пересчитываются при изменении разрешения. */
    private int cachedAnchoPantalla;
    private int cachedAltoPantalla;
    private int cachedX;
    private int cachedY;

    public SaciedadHudRenderizador(SaciedadConfig config) {
        this.config = config;
    }

    /**
     * Главный метод отрисовки фона шкалы (подписывается первым).
     *
     * <p>Рисует полупрозрачный прямоугольник-подложку полной ширины.
     *
     * @param evento событие HUD-кадра
     */
    public void renderizarFondo(LatidoRenderHud evento) {
        if (!debeRenderizar()) return;
        actualizarCache(evento.anchoPantalla(), evento.altoPantalla());

        PintorHud pintor = evento.pintor();
        pintor.dibujarRectangulo(cachedX, cachedY, ANCHO_MAX, ALTO_BARRA, COLOR_FONDO);
    }

    /**
     * Главный метод отрисовки самой шкалы (подписывается вторым).
     *
     * <p>Ширина шкалы пропорциональна {@link SaciedadCache#leer()}.
     *
     * @param evento событие HUD-кадра
     */
    public void renderizarBarra(LatidoRenderHud evento) {
        if (!debeRenderizar()) return;
        actualizarCache(evento.anchoPantalla(), evento.altoPantalla());

        float saturation = SaciedadCache.leer();
        int ancho = (int) (saturation / 20f * ANCHO_MAX);
        if (ancho <= 0) return;

        evento.pintor().dibujarRectangulo(cachedX, cachedY, ancho, ALTO_BARRA, config.color());
    }

    // ---------------------------------------------------------------- helpers

    private boolean debeRenderizar() {
        return config.mostrarSiempre() || SaciedadCache.leer() > 0f;
    }

    private void actualizarCache(int ancho, int alto) {
        if (ancho == cachedAnchoPantalla && alto == cachedAltoPantalla) return;
        cachedAnchoPantalla = ancho;
        cachedAltoPantalla = alto;
        cachedX = ancho / 2 - 91;
        int baseY = alto - OFFSET_HAMBRE_DESDE_ABAJO;
        cachedY = switch (config.posicion()) {
            case ARRIBA  -> baseY - OFFSET_VER;
            case ABAJO   -> baseY + OFFSET_VER;
            case ENCIMA  -> baseY;
        };
    }
}
