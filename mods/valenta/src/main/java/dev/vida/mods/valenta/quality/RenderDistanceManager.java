/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.quality;

import dev.vida.core.ApiStatus;

/**
 * Handles render distance changes without freezing chunk workers.
 *
 * <p>Vanilla Minecraft rebuilds all chunks synchronously when render distance
 * changes, causing multi-second freezes at low values (&lt; 8). This manager
 * implements a smooth transition by clamping the effective render distance
 * to a safe minimum and gradually adjusting over multiple frames.
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>Target render distance is set by the user.</li>
 *   <li>Effective distance moves by at most ±1 section per 4 frames toward target.</li>
 *   <li>If effective &lt; {@code minSafe}, sections outside the safe ring are unloaded
 *       lazily (one per frame) instead of all at once.</li>
 * </ol>
 */
@ApiStatus.Preview("valenta")
public final class RenderDistanceManager {

    private final int minSafe;
    private int target;
    private int effective;
    private int frameCounter;
    private static final int FRAMES_PER_STEP = 4;

    /**
     * @param minSafe  minimum safe render distance (in chunks)
     * @param initial  initial render distance
     */
    public RenderDistanceManager(int minSafe, int initial) {
        this.minSafe = Math.max(2, minSafe);
        this.target = Math.max(this.minSafe, initial);
        this.effective = this.target;
    }

    /**
     * Sets the desired render distance. The effective distance will gradually
     * adjust toward this target.
     */
    public void setTarget(int distance) {
        this.target = Math.max(minSafe, distance);
    }

    /**
     * Called once per frame to advance the smooth transition.
     */
    @ApiStatus.HotPath
    public void tick() {
        frameCounter++;
        if (frameCounter < FRAMES_PER_STEP) return;
        frameCounter = 0;

        if (effective < target) {
            effective++;
        } else if (effective > target) {
            effective--;
        }
    }

    /** Current effective render distance for this frame. */
    @ApiStatus.HotPath
    public int effective() {
        return effective;
    }

    public int target() { return target; }
    public int minSafe() { return minSafe; }
    public boolean isTransitioning() { return effective != target; }
}
