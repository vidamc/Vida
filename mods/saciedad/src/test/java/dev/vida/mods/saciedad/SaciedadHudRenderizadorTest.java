/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.saciedad;

import static org.assertj.core.api.Assertions.*;

import dev.vida.render.LatidoRenderHud;
import dev.vida.render.PintorHud;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Headless-тест рендерера HUD.
 *
 * <p>Вместо реального OpenGL используется {@link MockPintorHud} — он просто
 * записывает все вызовы в список. Тест проверяет геометрию и цвет,
 * не запуская никакого графического кода.
 */
final class SaciedadHudRenderizadorTest {

    /** Захватывает все вызовы {@link PintorHud#dibujarRectangulo}. */
    static final class MockPintorHud implements PintorHud {
        record Llamada(int x, int y, int ancho, int alto, int color) {}

        final List<Llamada> llamadas = new ArrayList<>();

        @Override
        public void dibujarRectangulo(int x, int y, int ancho, int alto, int colorArgb) {
            llamadas.add(new Llamada(x, y, ancho, alto, colorArgb));
        }
    }

    private static final int PANTALLA_ANCHO  = 1920;
    private static final int PANTALLA_ALTO   = 1080;
    private static final float DELTA         = 0.0f;

    private MockPintorHud pintor;

    @BeforeEach
    void setUp() {
        pintor = new MockPintorHud();
        SaciedadCache.actualizar(0f);
    }

    private LatidoRenderHud evento() {
        return new LatidoRenderHud(PANTALLA_ANCHO, PANTALLA_ALTO, DELTA, pintor);
    }

    // ---------------------------------------------------------------- mostrarSiempre = false

    @Test
    void noRenderiza_cuando_saturation_cero_y_noMostrarSiempre() {
        SaciedadHudRenderizador r = new SaciedadHudRenderizador(SaciedadConfig.defecto());
        SaciedadCache.actualizar(0f);

        r.renderizarFondo(evento());
        r.renderizarBarra(evento());

        assertThat(pintor.llamadas).isEmpty();
    }

    @Test
    void renderiza_fondo_cuando_saturation_positiva() {
        SaciedadHudRenderizador r = new SaciedadHudRenderizador(SaciedadConfig.defecto());
        SaciedadCache.actualizar(10f);

        r.renderizarFondo(evento());

        assertThat(pintor.llamadas).hasSize(1);
        MockPintorHud.Llamada llamada = pintor.llamadas.getFirst();
        assertThat(llamada.ancho()).isEqualTo(81);    // полная ширина для фона
        assertThat(llamada.alto()).isEqualTo(5);
        assertThat(llamada.color()).isEqualTo((int) 0x80000000L);
    }

    @Test
    void renderiza_barra_proporcionalnaKSaturation() {
        SaciedadHudRenderizador r = new SaciedadHudRenderizador(SaciedadConfig.defecto());
        SaciedadCache.actualizar(10f); // 10/20 = 50% → 81*0.5 = 40px

        r.renderizarBarra(evento());

        assertThat(pintor.llamadas).hasSize(1);
        assertThat(pintor.llamadas.getFirst().ancho()).isEqualTo(40);
    }

    @Test
    void renderiza_barra_maxWidth_atMaxSaturation() {
        SaciedadHudRenderizador r = new SaciedadHudRenderizador(SaciedadConfig.defecto());
        SaciedadCache.actualizar(20f);

        r.renderizarBarra(evento());

        assertThat(pintor.llamadas).hasSize(1);
        assertThat(pintor.llamadas.getFirst().ancho()).isEqualTo(81);
    }

    @Test
    void noRenderiza_barra_quando_anchoCero() {
        SaciedadHudRenderizador r = new SaciedadHudRenderizador(SaciedadConfig.defecto());
        // Saturation > 0 для renderizarFondo (чтоб он работал), но barra с floor=0
        SaciedadCache.actualizar(0.001f); // < 1/81 → int = 0

        r.renderizarBarra(evento());

        // Анча бара = (int)(0.001f / 20f * 81) = 0 → не рисуется
        assertThat(pintor.llamadas).isEmpty();
    }

    // ---------------------------------------------------------------- mostrarSiempre = true

    @Test
    void renderizaFondo_quando_mostrarSiempre_y_saturationCero() {
        SaciedadConfig cfg = new SaciedadConfig(
                (int) 0xFFFFA500L, true, SaciedadConfig.Posicion.ABAJO);
        SaciedadHudRenderizador r = new SaciedadHudRenderizador(cfg);
        SaciedadCache.actualizar(0f);

        r.renderizarFondo(evento());

        assertThat(pintor.llamadas).hasSize(1);
    }

    // ---------------------------------------------------------------- posicion

    @Test
    void posicion_abajo_y() {
        SaciedadConfig cfg = new SaciedadConfig(
                (int) 0xFFFFA500L, false, SaciedadConfig.Posicion.ABAJO);
        SaciedadHudRenderizador r = new SaciedadHudRenderizador(cfg);
        SaciedadCache.actualizar(20f);

        r.renderizarBarra(evento());

        int expectedY = PANTALLA_ALTO - 39 + 10;
        assertThat(pintor.llamadas.getFirst().y()).isEqualTo(expectedY);
    }

    @Test
    void posicion_arriba_y() {
        SaciedadConfig cfg = new SaciedadConfig(
                (int) 0xFFFFA500L, false, SaciedadConfig.Posicion.ARRIBA);
        SaciedadHudRenderizador r = new SaciedadHudRenderizador(cfg);
        SaciedadCache.actualizar(20f);

        r.renderizarBarra(evento());

        int expectedY = PANTALLA_ALTO - 39 - 10;
        assertThat(pintor.llamadas.getFirst().y()).isEqualTo(expectedY);
    }

    @Test
    void posicion_encima_y() {
        SaciedadConfig cfg = new SaciedadConfig(
                (int) 0xFFFFA500L, false, SaciedadConfig.Posicion.ENCIMA);
        SaciedadHudRenderizador r = new SaciedadHudRenderizador(cfg);
        SaciedadCache.actualizar(20f);

        r.renderizarBarra(evento());

        int expectedY = PANTALLA_ALTO - 39;
        assertThat(pintor.llamadas.getFirst().y()).isEqualTo(expectedY);
    }

    @Test
    void x_centeredOnHungerBar() {
        SaciedadHudRenderizador r = new SaciedadHudRenderizador(SaciedadConfig.defecto());
        SaciedadCache.actualizar(20f);

        r.renderizarBarra(evento());

        int expectedX = PANTALLA_ANCHO / 2 - 91;
        assertThat(pintor.llamadas.getFirst().x()).isEqualTo(expectedX);
    }

    // ---------------------------------------------------------------- color

    @Test
    void barra_usesConfigColor() {
        int testColor = (int) 0xFF00FF00L;
        SaciedadConfig cfg = new SaciedadConfig(testColor, false, SaciedadConfig.Posicion.ABAJO);
        SaciedadHudRenderizador r = new SaciedadHudRenderizador(cfg);
        SaciedadCache.actualizar(20f);

        r.renderizarBarra(evento());

        assertThat(pintor.llamadas.getFirst().color()).isEqualTo(testColor);
    }

    // ---------------------------------------------------------------- cache invalidation

    @Test
    void cache_recomputed_on_resolutionChange() {
        SaciedadHudRenderizador r = new SaciedadHudRenderizador(SaciedadConfig.defecto());
        SaciedadCache.actualizar(20f);

        LatidoRenderHud evento1 = new LatidoRenderHud(800, 600, DELTA, pintor);
        r.renderizarBarra(evento1);
        int x1 = pintor.llamadas.getFirst().x();

        pintor.llamadas.clear();
        LatidoRenderHud evento2 = new LatidoRenderHud(1920, 1080, DELTA, pintor);
        r.renderizarBarra(evento2);
        int x2 = pintor.llamadas.getFirst().x();

        assertThat(x1).isNotEqualTo(x2);
        assertThat(x1).isEqualTo(800 / 2 - 91);
        assertThat(x2).isEqualTo(1920 / 2 - 91);
    }
}
