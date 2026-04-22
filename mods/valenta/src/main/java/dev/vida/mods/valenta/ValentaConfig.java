/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta;

import dev.vida.base.ajustes.Ajuste;
import dev.vida.base.ajustes.AjustesTipados;
import dev.vida.core.ApiStatus;
import dev.vida.mods.valenta.quality.CloudRenderer;
import dev.vida.mods.valenta.quality.ParticleFilter;

/**
 * Configuration for Valenta, read from {@code valenta.toml}.
 *
 * <p>All settings have sensible defaults targeting modern hardware
 * (GL 4.3+, dedicated GPU). Older hardware can disable specific
 * features via the config file.
 */
@ApiStatus.Preview("valenta")
public record ValentaConfig(
        boolean useMultiDrawIndirect,
        boolean useCompactVertexFormat,
        boolean biomeBlendingSsbo,
        boolean blockLightSsbo,
        int meshWorkers,
        int maxColaPorEtiqueta,
        int renderDistance,
        boolean useOcclusionQueries,
        boolean useFrustumCulling,
        boolean usePvsTree,
        boolean invalidateWhenOccluded,
        ParticleFilter.Mode particles,
        CloudRenderer.Mode clouds,
        boolean animatedTextures,
        int minRenderDistanceSafe,
        boolean showGpuTimings,
        boolean showOcclusionOverlay) {

    // ---- Ajuste descriptors (keyed to valenta.toml paths) ----

    static final Ajuste<Boolean> A_MULTI_DRAW = Ajuste.logico("render.useMultiDrawIndirect", true).build();
    static final Ajuste<Boolean> A_COMPACT_VTX = Ajuste.logico("render.useCompactVertexFormat", true).build();
    static final Ajuste<Boolean> A_BIOME_SSBO = Ajuste.logico("render.biomeBlendingSsbo", true).build();
    static final Ajuste<Boolean> A_LIGHT_SSBO = Ajuste.logico("render.blockLightSsbo", true).build();

    static final Ajuste<Integer> A_MESH_WORKERS = Ajuste.entero("chunks.meshWorkers", 0).build();
    static final Ajuste<Integer> A_MAX_COLA = Ajuste.entero("chunks.maxColaPorEtiqueta", 64).build();
    static final Ajuste<Integer> A_RENDER_DIST = Ajuste.entero("chunks.renderDistance", 0).build();

    static final Ajuste<Boolean> A_OCCLUSION = Ajuste.logico("culling.useOcclusionQueries", true).build();
    static final Ajuste<Boolean> A_FRUSTUM = Ajuste.logico("culling.useFrustumCulling", true).build();
    static final Ajuste<Boolean> A_PVS = Ajuste.logico("culling.usePvsTree", true).build();

    static final Ajuste<Boolean> A_SKY_INVALIDATE = Ajuste.logico("sky.invalidateWhenOccluded", true).build();

    static final Ajuste<String> A_PARTICLES = Ajuste.cadena("quality.particles", "none").build();
    static final Ajuste<String> A_CLOUDS = Ajuste.cadena("quality.clouds", "vanilla").build();
    static final Ajuste<Boolean> A_ANIMATED = Ajuste.logico("quality.animatedTextures", true).build();
    static final Ajuste<Integer> A_MIN_RD_SAFE = Ajuste.entero("quality.minRenderDistanceSafe", 2).build();

    static final Ajuste<Boolean> A_GPU_TIMINGS = Ajuste.logico("debug.showGpuTimings", false).build();
    static final Ajuste<Boolean> A_OCCLUSION_OVERLAY = Ajuste.logico("debug.showOcclusionOverlay", false).build();

    /** Defaults targeting modern GL 4.3+ hardware. */
    public static ValentaConfig defecto() {
        return new ValentaConfig(
                true, true, true, true,
                0, 64, 0,
                true, true, true,
                true,
                ParticleFilter.Mode.NINGUNO,
                CloudRenderer.Mode.VANILLA,
                true, 2,
                false, false);
    }

    /**
     * Reads configuration from typed settings.
     */
    public static ValentaConfig desde(AjustesTipados tipados) {
        int workers = tipados.valor(A_MESH_WORKERS);
        if (workers <= 0) {
            workers = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        }
        return new ValentaConfig(
                tipados.valor(A_MULTI_DRAW),
                tipados.valor(A_COMPACT_VTX),
                tipados.valor(A_BIOME_SSBO),
                tipados.valor(A_LIGHT_SSBO),
                workers,
                tipados.valor(A_MAX_COLA),
                tipados.valor(A_RENDER_DIST),
                tipados.valor(A_OCCLUSION),
                tipados.valor(A_FRUSTUM),
                tipados.valor(A_PVS),
                tipados.valor(A_SKY_INVALIDATE),
                ParticleFilter.Mode.parse(tipados.valor(A_PARTICLES)),
                CloudRenderer.Mode.parse(tipados.valor(A_CLOUDS)),
                tipados.valor(A_ANIMATED),
                tipados.valor(A_MIN_RD_SAFE),
                tipados.valor(A_GPU_TIMINGS),
                tipados.valor(A_OCCLUSION_OVERLAY));
    }
}
