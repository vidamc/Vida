/*
 * Compilation stub only — NOT included in the loader JAR (excluded by
 * :loader:jar and :loader:agentJar).
 *
 * Provides just enough API surface for dev.vida.platform.GuiRenderMorph
 * to compile. At runtime, Minecraft supplies the real class.
 */
package net.minecraft.client.gui;

/**
 * Stub for {@code net.minecraft.client.gui.GuiGraphics} (Minecraft 1.20.5+).
 *
 * <p>The real class exposes dozens of methods; we declare only the ones used
 * by {@code GuiRenderMorph} to dispatch {@code LatidoRenderHud}.
 */
public class GuiGraphics {

    /**
     * Draws a filled rectangle.
     *
     * @param minX   left edge (inclusive)
     * @param minY   top edge (inclusive)
     * @param maxX   right edge (exclusive)
     * @param maxY   bottom edge (exclusive)
     * @param color  color in ARGB format
     */
    public void fill(int minX, int minY, int maxX, int maxY, int color) {
        // stub — replaced by the real implementation at runtime
    }
}
