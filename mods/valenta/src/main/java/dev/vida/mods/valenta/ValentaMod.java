/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta;

import dev.vida.base.ModContext;
import dev.vida.base.VidaEntrypoint;
import dev.vida.base.VidaMod;
import dev.vida.base.latidos.Fase;
import dev.vida.base.latidos.Prioridad;
import dev.vida.base.latidos.Suscripcion;
import dev.vida.core.ApiStatus;
import dev.vida.mods.valenta.chunk.AnalisisEtapa;
import dev.vida.mods.valenta.chunk.BuildEtapa;
import dev.vida.mods.valenta.chunk.ChunkTaskGraph;
import dev.vida.mods.valenta.core.BiomeBlendSsbo;
import dev.vida.mods.valenta.core.BlockLightSsbo;
import dev.vida.mods.valenta.core.GlFunctions;
import dev.vida.mods.valenta.core.VboMallaBatcher;
import dev.vida.mods.valenta.culling.CullingEngine;
import dev.vida.mods.valenta.culling.PvsTree;
import dev.vida.mods.valenta.debug.OcclusionOverlay;
import dev.vida.mods.valenta.debug.ValentaDebugComando;
import dev.vida.mods.valenta.escultor.ValentaHooks;
import dev.vida.mods.valenta.quality.AnimatedTextureManager;
import dev.vida.mods.valenta.quality.CloudRenderer;
import dev.vida.mods.valenta.quality.GpuTimingPane;
import dev.vida.mods.valenta.quality.ParticleFilter;
import dev.vida.mods.valenta.quality.RenderDistanceManager;
import dev.vida.mods.valenta.sky.SkyRenderer;
import dev.vida.render.LatidoRenderHud;
import dev.vida.susurro.Susurro;

/**
 * Valenta — Sodium-class rendering optimization mod.
 *
 * <p>Pipeline: render core, chunk meshing (Susurro), culling, QoL, Vifada
 * escultor morphs. See {@code docs/mods/valenta/architecture.md} in the repo
 * for the full diagram and data flow.
 */
@ApiStatus.Preview("valenta")
@VidaEntrypoint
public final class ValentaMod implements VidaMod, ValentaHooks.ValentaHookReceiver {

    private ValentaConfig config;
    private GlFunctions gl;
    private VboMallaBatcher batcher;
    private BiomeBlendSsbo biomeBlend;
    private BlockLightSsbo blockLight;
    private CullingEngine culling;
    private SkyRenderer skyRenderer;
    private ParticleFilter particleFilter;
    private CloudRenderer cloudRenderer;
    private AnimatedTextureManager animatedTextures;
    private RenderDistanceManager renderDistanceManager;
    private GpuTimingPane timingPane;
    private OcclusionOverlay occlusionOverlay;
    private ValentaDebugComando debugComando;
    private ChunkTaskGraph chunkGraph;
    private Susurro susurro;

    private Suscripcion suscripcionOverlay;
    private boolean chunkPipelineActive;

    @Override
    public void iniciar(ModContext ctx) {
        config = ValentaConfig.desde(ctx.ajustes());
        gl = new GlFunctions.Noop();

        batcher = new VboMallaBatcher(gl, 65536, 98304);

        if (config.biomeBlendingSsbo()) {
            biomeBlend = new BiomeBlendSsbo(gl);
        }
        if (config.blockLightSsbo()) {
            blockLight = new BlockLightSsbo(gl);
        }

        culling = new CullingEngine(gl, PvsTree.empty(),
                config.useOcclusionQueries(), config.usePvsTree());

        skyRenderer = new SkyRenderer(gl, config.invalidateWhenOccluded());

        particleFilter = new ParticleFilter(config.particles(), 4);
        cloudRenderer = new CloudRenderer(config.clouds(), 192);
        animatedTextures = new AnimatedTextureManager(config.animatedTextures());
        renderDistanceManager = new RenderDistanceManager(
                config.minRenderDistanceSafe(),
                config.renderDistance() > 0 ? config.renderDistance() : 12);

        timingPane = new GpuTimingPane(config.showGpuTimings());
        occlusionOverlay = new OcclusionOverlay(config.showOcclusionOverlay());
        debugComando = new ValentaDebugComando(timingPane, occlusionOverlay);

        susurro = Susurro.iniciar(
                new Susurro.Politica(config.meshWorkers(), 1024, config.maxColaPorEtiqueta()));
        AnalisisEtapa analisis = new AnalisisEtapa(128);
        BuildEtapa build = new BuildEtapa(config.biomeBlendingSsbo(), config.blockLightSsbo());
        chunkGraph = new ChunkTaskGraph(susurro, analisis, build, new NoopSnapshot());
        chunkPipelineActive = true;

        suscripcionOverlay = ctx.latidos().suscribir(
                LatidoRenderHud.TIPO,
                Prioridad.BAJA,
                Fase.DESPUES,
                evento -> occlusionOverlay.renderizar(evento, culling));

        ValentaHooks.install(this);

        ctx.log().info("Valenta iniciado (multiDraw={}, compactVtx={}, occlusion={}, pvs={})",
                config.useMultiDrawIndirect(), config.useCompactVertexFormat(),
                config.useOcclusionQueries(), config.usePvsTree());
    }

    @Override
    public void detener(ModContext ctx) {
        ValentaHooks.uninstall();
        if (suscripcionOverlay != null) suscripcionOverlay.cancelar();
        if (chunkGraph != null) chunkGraph.cancelAll();
        if (susurro != null) susurro.detener();
        if (batcher != null) batcher.close();
        if (biomeBlend != null) biomeBlend.close();
        if (blockLight != null) blockLight.close();
        if (culling != null) culling.close();
    }

    // ---- ValentaHookReceiver implementation ----

    @Override
    @ApiStatus.HotPath
    public void onSetupRender(Object viewArea, Object camera, Object frustum) {
        renderDistanceManager.tick();
    }

    @Override
    @ApiStatus.HotPath
    public void onRenderLevelStart(float partialTick, long nanoTime) {
        timingPane.beginFrame();
    }

    @Override
    @ApiStatus.HotPath
    public void onGameRendererStart(float partialTick, long nanoTime) {
        timingPane.recordPassSince("setup", nanoTime);
    }

    @Override
    @ApiStatus.HotPath
    public Float getEffectiveRenderDistance() {
        return (float) (renderDistanceManager.effective() * 16);
    }

    @Override
    public boolean isChunkPipelineActive() {
        return chunkPipelineActive;
    }

    @Override
    @ApiStatus.HotPath
    public void onChunkDispatcherTick() {
        // Collect and flush completed chunk meshes handled by ChunkTaskGraph
    }

    @Override
    @ApiStatus.HotPath
    public boolean shouldRenderParticle() {
        return particleFilter.shouldRender();
    }

    @Override
    @ApiStatus.HotPath
    public boolean shouldRenderClouds() {
        return cloudRenderer.shouldRender();
    }

    // ---- Accessors for tests and debug ----

    ValentaConfig config() { return config; }
    CullingEngine culling() { return culling; }
    ValentaDebugComando debugComando() { return debugComando; }
    GpuTimingPane timingPane() { return timingPane; }
    ParticleFilter particleFilter() { return particleFilter; }
    CloudRenderer cloudRenderer() { return cloudRenderer; }
    AnimatedTextureManager animatedTextures() { return animatedTextures; }
    RenderDistanceManager renderDistanceManager() { return renderDistanceManager; }
    SkyRenderer skyRenderer() { return skyRenderer; }
    ChunkTaskGraph chunkGraph() { return chunkGraph; }

    /**
     * No-op snapshot for initialization; replaced by a real provider
     * once the MC world is loaded.
     */
    private static final class NoopSnapshot implements BuildEtapa.SectionSnapshot {
        @Override public int blockStateAt(int x, int y, int z) { return 0; }
        @Override public boolean isOpaque(int x, int y, int z) { return false; }
        @Override public int biomeColor(int x, int z) { return 0; }
        @Override public int lightAt(int x, int y, int z) { return 0; }
    }
}
