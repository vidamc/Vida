/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.debug;

import dev.vida.core.ApiStatus;
import dev.vida.mods.valenta.culling.CullingEngine;
import dev.vida.mods.valenta.quality.GpuTimingPane;
import java.util.Map;
import java.util.Objects;

/**
 * Handles {@code /valenta debug} subcommands:
 *
 * <ul>
 *   <li>{@code /valenta debug occlusion} — toggles the occlusion overlay</li>
 *   <li>{@code /valenta debug gpu} — toggles the GPU timing pane</li>
 *   <li>{@code /valenta debug stats} — prints current frame statistics</li>
 * </ul>
 */
@ApiStatus.Internal
public final class ValentaDebugComando {

    private final GpuTimingPane timingPane;
    private final OcclusionOverlay occlusionOverlay;

    public ValentaDebugComando(GpuTimingPane timingPane, OcclusionOverlay occlusionOverlay) {
        this.timingPane = Objects.requireNonNull(timingPane, "timingPane");
        this.occlusionOverlay = Objects.requireNonNull(occlusionOverlay, "occlusionOverlay");
    }

    /**
     * Executes a debug subcommand.
     *
     * @param subCommand the subcommand after "debug"
     * @param culling    current culling engine (for stats)
     * @return human-readable response message
     */
    public String ejecutar(String subCommand, CullingEngine culling) {
        Objects.requireNonNull(subCommand, "subCommand");
        return switch (subCommand.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "occlusion" -> {
                boolean now = !occlusionOverlay.isVisible();
                occlusionOverlay.setVisible(now);
                yield "Occlusion overlay: " + (now ? "ON" : "OFF");
            }
            case "gpu" -> {
                boolean now = !timingPane.isVisible();
                timingPane.setVisible(now);
                yield "GPU timing pane: " + (now ? "ON" : "OFF");
            }
            case "stats" -> formatStats(culling);
            default -> "Subcomandos: occlusion | gpu | stats";
        };
    }

    private String formatStats(CullingEngine culling) {
        StringBuilder sb = new StringBuilder("[Valenta Stats]\n");
        if (culling != null) {
            sb.append("  tested    : ").append(culling.testedCount()).append('\n');
            sb.append("  visible   : ").append(culling.visibleCount()).append('\n');
            sb.append("  frustum   : -").append(culling.frustumCulled()).append('\n');
            sb.append("  pvs       : -").append(culling.pvsCulled()).append('\n');
            sb.append("  occlusion : -").append(culling.occlusionCulled()).append('\n');
        }
        Map<String, String> timings = timingPane.formatTimings();
        if (!timings.isEmpty()) {
            sb.append("[GPU Timings]\n");
            timings.forEach((k, v) -> sb.append("  ").append(k).append(" : ").append(v).append('\n'));
        }
        return sb.toString();
    }
}
