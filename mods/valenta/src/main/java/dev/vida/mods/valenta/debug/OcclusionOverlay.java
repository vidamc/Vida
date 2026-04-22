/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.debug;

import dev.vida.core.ApiStatus;
import dev.vida.mods.valenta.culling.CullingEngine;
import dev.vida.render.LatidoRenderHud;
import dev.vida.render.PintorHud;

/**
 * Debug overlay that visualizes culling decisions.
 *
 * <p>When active, renders a small info box on the HUD showing the
 * current culling statistics: sections tested, visible, culled by
 * frustum/PVS/occlusion.
 */
@ApiStatus.Internal
public final class OcclusionOverlay {

    private static final int BG_COLOR = 0xAA000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int CULLED_COLOR = 0xFFFF4444;
    private static final int VISIBLE_COLOR = 0xFF44FF44;

    private boolean visible;

    public OcclusionOverlay(boolean visible) {
        this.visible = visible;
    }

    /**
     * Renders the overlay onto the HUD.
     *
     * @param evento  HUD render event
     * @param culling current culling engine
     */
    public void renderizar(LatidoRenderHud evento, CullingEngine culling) {
        if (!visible || culling == null) return;

        PintorHud pintor = evento.pintor();
        int x = evento.anchoPantalla() - 160;
        int y = 4;
        int lineHeight = 10;

        pintor.dibujarRectangulo(x - 4, y - 2, 156, lineHeight * 5 + 4, BG_COLOR);

        int visibleRatio = culling.testedCount() > 0
                ? (culling.visibleCount() * 100 / culling.testedCount()) : 100;

        int barWidth = 148;
        int greenWidth = barWidth * visibleRatio / 100;
        pintor.dibujarRectangulo(x, y, greenWidth, 6, VISIBLE_COLOR);
        pintor.dibujarRectangulo(x + greenWidth, y, barWidth - greenWidth, 6, CULLED_COLOR);
        y += lineHeight;

        pintor.dibujarRectangulo(x, y, 8, 8, VISIBLE_COLOR);
        y += lineHeight;

        pintor.dibujarRectangulo(x, y, 8, 8, CULLED_COLOR);
        y += lineHeight;

        pintor.dibujarRectangulo(x, y, 60, 6, 0xFF4444FF);
        y += lineHeight;

        pintor.dibujarRectangulo(x, y, 60, 6, 0xFFFF8800);
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
}
