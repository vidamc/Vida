/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.quality;

import dev.vida.core.ApiStatus;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * F3 custom debug pane displaying GPU pass timings.
 *
 * <p>Each render pass records its CPU-side timing; the pane formats
 * this into a readable table overlaid on the debug screen when
 * {@code /valenta debug} is active or configured in {@code valenta.toml}.
 *
 * <h2>Example output</h2>
 * <pre>
 * [Valenta GPU]
 *   terrain   : 2.31 ms
 *   entities  : 0.87 ms
 *   particles : 0.12 ms
 *   upload    : 0.45 ms
 *   total     : 3.75 ms
 * </pre>
 */
@ApiStatus.Preview("valenta")
public final class GpuTimingPane {

    private final LinkedHashMap<String, Long> currentTimings = new LinkedHashMap<>();
    private final LinkedHashMap<String, Long> displayTimings = new LinkedHashMap<>();
    private boolean visible;
    private long frameStartNanos;

    public GpuTimingPane(boolean visible) {
        this.visible = visible;
    }

    /**
     * Starts timing for a new frame.
     */
    @ApiStatus.HotPath
    public void beginFrame() {
        currentTimings.clear();
        frameStartNanos = System.nanoTime();
    }

    /**
     * Records timing for a named pass.
     *
     * @param passName name of the render pass
     * @param nanos    duration in nanoseconds
     */
    @ApiStatus.HotPath
    public void recordPass(String passName, long nanos) {
        currentTimings.merge(passName, nanos, Long::sum);
    }

    /**
     * Convenience: records a pass by measuring between start and now.
     *
     * @param passName  pass name
     * @param startNano timestamp from {@link System#nanoTime()} at pass start
     */
    @ApiStatus.HotPath
    public void recordPassSince(String passName, long startNano) {
        recordPass(passName, System.nanoTime() - startNano);
    }

    /**
     * Ends frame timing and makes results available for display.
     */
    @ApiStatus.HotPath
    public void endFrame() {
        long totalNanos = System.nanoTime() - frameStartNanos;
        displayTimings.clear();
        displayTimings.putAll(currentTimings);
        displayTimings.put("total", totalNanos);
    }

    /**
     * @return formatted timing lines for the debug overlay
     */
    public Map<String, String> formatTimings() {
        if (!visible || displayTimings.isEmpty()) return Map.of();
        Map<String, String> result = new LinkedHashMap<>();
        for (var entry : displayTimings.entrySet()) {
            double ms = entry.getValue() / 1_000_000.0;
            result.put(entry.getKey(), String.format("%.2f ms", ms));
        }
        return Collections.unmodifiableMap(result);
    }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    /**
     * @return raw timings in nanoseconds for the last completed frame
     */
    public Map<String, Long> rawTimings() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(displayTimings));
    }
}
