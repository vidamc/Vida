/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.quality;

import dev.vida.core.ApiStatus;
import java.util.Locale;
import java.util.Objects;

/**
 * Controls particle rendering: none (vanilla), reduce, or hide all.
 *
 * <p>When set to {@link Mode#REDUCIR}, only every Nth particle is emitted
 * (N configurable). When set to {@link Mode#OCULTAR}, the particle engine
 * is effectively bypassed.
 */
@ApiStatus.Preview("valenta")
public final class ParticleFilter {

    public enum Mode {
        /** Vanilla particle behavior. */
        NINGUNO,
        /** Show every Nth particle. */
        REDUCIR,
        /** Hide all particles. */
        OCULTAR;

        public static Mode parse(String raw) {
            return switch (Objects.requireNonNull(raw).trim().toLowerCase(Locale.ROOT)) {
                case "reducir" -> REDUCIR;
                case "ocultar" -> OCULTAR;
                default -> NINGUNO;
            };
        }
    }

    private Mode mode;
    private int reduceRatio;
    private long counter;
    private long filtered;

    /**
     * @param mode         particle filter mode
     * @param reduceRatio  emit every Nth particle in REDUCIR mode (must be >= 2)
     */
    public ParticleFilter(Mode mode, int reduceRatio) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.reduceRatio = Math.max(2, reduceRatio);
    }

    /**
     * Tests whether the next particle should be rendered.
     *
     * @return true if the particle should be rendered
     */
    @ApiStatus.HotPath
    public boolean shouldRender() {
        return switch (mode) {
            case NINGUNO -> true;
            case REDUCIR -> {
                counter++;
                boolean show = (counter % reduceRatio) == 0;
                if (!show) filtered++;
                yield show;
            }
            case OCULTAR -> {
                filtered++;
                yield false;
            }
        };
    }

    public void setMode(Mode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
        counter = 0;
    }

    public void setReduceRatio(int ratio) {
        this.reduceRatio = Math.max(2, ratio);
    }

    public Mode mode() { return mode; }
    public int reduceRatio() { return reduceRatio; }
    public long filteredCount() { return filtered; }

    public void resetStats() {
        counter = 0;
        filtered = 0;
    }
}
