/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.chunk;

import dev.vida.core.ApiStatus;
import dev.vida.mods.valenta.chunk.AnalisisEtapa.SectionRequest;
import dev.vida.susurro.Etiqueta;
import dev.vida.susurro.Prioridad;
import dev.vida.susurro.Susurro;
import dev.vida.susurro.Tarea;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Orchestrates the three-stage chunk meshing pipeline using
 * {@link Susurro} for parallel execution.
 *
 * <pre>
 *   ┌──────────┐      ┌──────────┐      ┌──────────┐
 *   │ Analisis │─────▶│  Build   │─────▶│  Upload  │
 *   │ (render) │      │ (worker) │      │ (render) │
 *   └──────────┘      └──────────┘      └──────────┘
 * </pre>
 *
 * <p><b>Analisis</b> runs on the render thread and produces a sorted list
 * of section requests. <b>Build</b> runs on Susurro workers with
 * {@code Etiqueta.de("valenta/chunk")} for back-pressure. <b>Upload</b>
 * runs on the render thread collecting completed builds.
 */
@ApiStatus.Preview("valenta")
public final class ChunkTaskGraph {

    public static final Etiqueta ETIQUETA = Etiqueta.de("valenta/chunk");

    private final Susurro susurro;
    private final AnalisisEtapa analisis;
    private final BuildEtapa build;
    private final BuildEtapa.SectionSnapshot snapshotProvider;
    private final ConcurrentLinkedQueue<MallaChunk> completed = new ConcurrentLinkedQueue<>();
    private final List<Tarea<MallaChunk>> pendingTasks = new ArrayList<>();

    /**
     * @param susurro          shared Susurro pool
     * @param analisis         analysis stage
     * @param build            build stage
     * @param snapshotProvider global section data provider
     */
    public ChunkTaskGraph(Susurro susurro, AnalisisEtapa analisis,
                          BuildEtapa build, BuildEtapa.SectionSnapshot snapshotProvider) {
        this.susurro = Objects.requireNonNull(susurro, "susurro");
        this.analisis = Objects.requireNonNull(analisis, "analisis");
        this.build = Objects.requireNonNull(build, "build");
        this.snapshotProvider = Objects.requireNonNull(snapshotProvider, "snapshotProvider");
    }

    /**
     * Runs the analysis stage on the render thread and dispatches
     * build tasks to Susurro workers.
     *
     * @param dirtySections dirty section coordinates
     * @param cameraX       camera world X
     * @param cameraY       camera world Y
     * @param cameraZ       camera world Z
     * @param renderDistance render distance in sections
     */
    public void dispatch(List<int[]> dirtySections,
                         double cameraX, double cameraY, double cameraZ,
                         int renderDistance) {
        List<SectionRequest> requests = analisis.analizar(
                dirtySections, cameraX, cameraY, cameraZ, renderDistance);

        for (SectionRequest req : requests) {
            Prioridad prio = req.priority() >= 2 ? Prioridad.ALTA : Prioridad.NORMAL;
            Tarea<MallaChunk> tarea = susurro.lanzar(prio, ETIQUETA, () -> {
                MallaChunk result = build.construir(req, snapshotProvider);
                if (result != null) {
                    completed.offer(result);
                }
                return result;
            });
            pendingTasks.add(tarea);
        }
    }

    /**
     * Collects completed meshes (non-blocking). Called on the render thread
     * before the upload stage.
     *
     * @return list of completed meshes, possibly empty
     */
    @ApiStatus.HotPath
    public List<MallaChunk> collect() {
        pendingTasks.removeIf(Tarea::terminada);
        List<MallaChunk> result = new ArrayList<>();
        MallaChunk m;
        while ((m = completed.poll()) != null) {
            result.add(m);
        }
        return result;
    }

    /**
     * Cancels all pending tasks. Called during cleanup or config reload.
     */
    public void cancelAll() {
        for (Tarea<MallaChunk> t : pendingTasks) {
            t.cancelar();
        }
        pendingTasks.clear();
        completed.clear();
    }

    public int pendingTaskCount() { return pendingTasks.size(); }
    public int completedCount() { return completed.size(); }
}
