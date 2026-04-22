/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.quality;

import dev.vida.core.ApiStatus;
import java.util.Locale;
import java.util.Objects;

/**
 * Cloud rendering control: vanilla, fast (simplified), or disabled.
 *
 * <p>{@link Mode#RAPIDAS} replaces volumetric clouds with flat textured
 * planes at cloud height, saving significant fill rate. Especially
 * effective on integrated GPUs.
 */
@ApiStatus.Preview("valenta")
public final class CloudRenderer {

    public enum Mode {
        /** Standard vanilla clouds. */
        VANILLA,
        /** Flat simplified clouds. */
        RAPIDAS,
        /** No clouds at all. */
        DESACTIVAR;

        public static Mode parse(String raw) {
            return switch (Objects.requireNonNull(raw).trim().toLowerCase(Locale.ROOT)) {
                case "rapidas" -> RAPIDAS;
                case "desactivar" -> DESACTIVAR;
                default -> VANILLA;
            };
        }
    }

    private Mode mode;
    private int cloudHeight;
    private long renderedFrames;
    private long skippedFrames;

    /**
     * @param mode        cloud rendering mode
     * @param cloudHeight Y level for cloud rendering (vanilla default: 192)
     */
    public CloudRenderer(Mode mode, int cloudHeight) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.cloudHeight = cloudHeight;
    }

    /**
     * @return true if clouds should be rendered this frame
     */
    @ApiStatus.HotPath
    public boolean shouldRender() {
        if (mode == Mode.DESACTIVAR) {
            skippedFrames++;
            return false;
        }
        renderedFrames++;
        return true;
    }

    /**
     * @return true if fast (flat) clouds should be used instead of vanilla
     */
    public boolean useFastClouds() {
        return mode == Mode.RAPIDAS;
    }

    public void setMode(Mode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    public Mode mode() { return mode; }
    public int cloudHeight() { return cloudHeight; }
    public void setCloudHeight(int height) { this.cloudHeight = height; }
    public long renderedFrames() { return renderedFrames; }
    public long skippedFrames() { return skippedFrames; }
}
