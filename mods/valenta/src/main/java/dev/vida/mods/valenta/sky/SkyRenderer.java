/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.sky;

import dev.vida.core.ApiStatus;
import dev.vida.mods.valenta.core.GlFunctions;
import dev.vida.mods.valenta.culling.ValentaFrustum;
import java.util.Objects;

/**
 * Optimized sky rendering that avoids redundant framebuffer clears.
 *
 * <p>When the camera is fully enclosed by opaque geometry (underground,
 * inside buildings), the sky is not visible and the color/depth clears
 * can be replaced with {@code glInvalidateFramebuffer}. This saves
 * significant bandwidth on tiled-rendering GPUs (mobile, integrated).
 *
 * <h2>Detection</h2>
 * The sky-visibility test uses a simple upward ray-march from the camera
 * position through section data. If all upward sections until the
 * world ceiling are opaque, the sky is considered fully occluded.
 *
 * <h2>Fallback</h2>
 * When invalidation is disabled (config or unsupported GPU), the standard
 * {@code glClear} path is used without modification.
 */
@ApiStatus.Preview("valenta")
public final class SkyRenderer {

    private static final int GL_COLOR = 0x1800;
    private static final int GL_FRAMEBUFFER = 0x8D40;

    /**
     * Provider for checking whether sections above the camera are opaque.
     */
    public interface SkyOcclusionProvider {
        /**
         * @return true if the section at (sectionX, sectionY, sectionZ) is
         *         entirely composed of opaque blocks (no sky-visible openings)
         */
        boolean isSectionFullyOpaque(int sectionX, int sectionY, int sectionZ);

        /**
         * @return maximum section Y coordinate in the world
         */
        int maxSectionY();
    }

    private final GlFunctions gl;
    private final boolean enabled;
    private boolean skyOccluded;
    private int invalidatedFrames;
    private int clearedFrames;

    /**
     * @param gl      GL functions
     * @param enabled whether glInvalidateFramebuffer optimization is enabled
     */
    public SkyRenderer(GlFunctions gl, boolean enabled) {
        this.gl = Objects.requireNonNull(gl, "gl");
        this.enabled = enabled;
    }

    /**
     * Tests sky visibility for the current frame. Call once per frame
     * before the clear/invalidate decision.
     *
     * @param provider    opacity data source
     * @param cameraSectionX camera section X
     * @param cameraSectionY camera section Y
     * @param cameraSectionZ camera section Z
     */
    public void updateVisibility(SkyOcclusionProvider provider,
                                 int cameraSectionX, int cameraSectionY,
                                 int cameraSectionZ) {
        Objects.requireNonNull(provider, "provider");
        if (!enabled) {
            skyOccluded = false;
            return;
        }

        for (int sy = cameraSectionY; sy <= provider.maxSectionY(); sy++) {
            if (!provider.isSectionFullyOpaque(cameraSectionX, sy, cameraSectionZ)) {
                skyOccluded = false;
                return;
            }
        }
        skyOccluded = true;
    }

    /**
     * Performs the framebuffer prepare step: either invalidate (if sky occluded)
     * or signal that a standard clear is needed.
     *
     * @return true if the caller should skip the standard glClear
     */
    @ApiStatus.HotPath
    public boolean prepareFramebuffer() {
        if (enabled && skyOccluded) {
            gl.glInvalidateFramebuffer(GL_FRAMEBUFFER, GL_COLOR);
            invalidatedFrames++;
            return true;
        }
        clearedFrames++;
        return false;
    }

    public boolean isSkyOccluded() { return skyOccluded; }
    public boolean isEnabled() { return enabled; }
    public int invalidatedFrames() { return invalidatedFrames; }
    public int clearedFrames() { return clearedFrames; }
}
