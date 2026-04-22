/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.quality;

import dev.vida.core.ApiStatus;

/**
 * Controls whether animated textures (water, lava, fire, portals) are ticked.
 *
 * <p>Disabling animated textures saves CPU time on texture atlas updates
 * (~0.3 ms/frame on mid-range hardware). Textures freeze on their current
 * frame instead of disappearing.
 */
@ApiStatus.Preview("valenta")
public final class AnimatedTextureManager {

    private boolean enabled;
    private long ticksSkipped;

    public AnimatedTextureManager(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return true if animated textures should tick this frame
     */
    @ApiStatus.HotPath
    public boolean shouldTick() {
        if (!enabled) {
            ticksSkipped++;
            return false;
        }
        return true;
    }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long ticksSkipped() { return ticksSkipped; }
}
