/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.culling;

import dev.vida.core.ApiStatus;
import dev.vida.mods.valenta.core.GlFunctions;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Combined culling engine: frustum + occlusion queries + PVS.
 *
 * <p>Each frame, sections pass through a three-tier test:
 * <ol>
 *   <li><b>PVS</b> — eliminates sections not in the potentially visible set.</li>
 *   <li><b>Frustum</b> — eliminates sections outside the view frustum.</li>
 *   <li><b>Occlusion queries</b> — eliminates sections hidden behind geometry.</li>
 * </ol>
 *
 * <p>Statistics are tracked for the debug overlay.
 */
@ApiStatus.Preview("valenta")
public final class CullingEngine implements AutoCloseable {

    private final ValentaFrustum frustum;
    private final PvsTree pvs;
    private final boolean useOcclusion;
    private final boolean usePvs;
    private final GlFunctions gl;
    private final Map<Long, OcclusionQuery> queries = new HashMap<>();

    private int cameraSectionX, cameraSectionY, cameraSectionZ;
    private int testedCount, frustumCulled, pvsCulled, occlusionCulled, visibleCount;

    /**
     * @param gl           GL functions
     * @param pvs          PVS tree (or {@link PvsTree#empty()})
     * @param useOcclusion enable hardware occlusion queries
     * @param usePvs       enable PVS-based culling
     */
    public CullingEngine(GlFunctions gl, PvsTree pvs,
                         boolean useOcclusion, boolean usePvs) {
        this.gl = Objects.requireNonNull(gl, "gl");
        this.pvs = Objects.requireNonNull(pvs, "pvs");
        this.frustum = new ValentaFrustum();
        this.useOcclusion = useOcclusion;
        this.usePvs = usePvs;
    }

    /**
     * Prepares for a new frame. Must be called once per frame before testing.
     *
     * @param mvpMatrix    column-major 4×4 MVP matrix
     * @param cameraSectionX camera section X
     * @param cameraSectionY camera section Y
     * @param cameraSectionZ camera section Z
     */
    public void beginFrame(double[] mvpMatrix,
                           int cameraSectionX, int cameraSectionY, int cameraSectionZ) {
        frustum.update(mvpMatrix);
        this.cameraSectionX = cameraSectionX;
        this.cameraSectionY = cameraSectionY;
        this.cameraSectionZ = cameraSectionZ;
        testedCount = 0;
        frustumCulled = 0;
        pvsCulled = 0;
        occlusionCulled = 0;
        visibleCount = 0;
    }

    /**
     * Tests whether a section should be rendered this frame.
     *
     * @return true if the section is visible and should be drawn
     */
    @ApiStatus.HotPath
    public boolean testSection(int sectionX, int sectionY, int sectionZ) {
        testedCount++;

        if (usePvs && !pvs.isPotentiallyVisible(
                cameraSectionX, cameraSectionY, cameraSectionZ,
                sectionX, sectionY, sectionZ)) {
            pvsCulled++;
            return false;
        }

        if (!frustum.testSection(sectionX, sectionY, sectionZ)) {
            frustumCulled++;
            return false;
        }

        if (useOcclusion) {
            long key = PvsTree.packKey(sectionX, sectionY, sectionZ);
            OcclusionQuery query = queries.get(key);
            if (query != null && !query.isVisible()) {
                occlusionCulled++;
                return false;
            }
        }

        visibleCount++;
        return true;
    }

    /**
     * Begins an occlusion query for the given section. Called before
     * rendering the bounding-box proxy geometry.
     */
    public void beginOcclusionQuery(int sectionX, int sectionY, int sectionZ) {
        if (!useOcclusion) return;
        long key = PvsTree.packKey(sectionX, sectionY, sectionZ);
        OcclusionQuery query = queries.computeIfAbsent(key, k -> new OcclusionQuery(gl));
        query.begin();
    }

    /**
     * Ends the current occlusion query.
     */
    public void endOcclusionQuery(int sectionX, int sectionY, int sectionZ) {
        if (!useOcclusion) return;
        long key = PvsTree.packKey(sectionX, sectionY, sectionZ);
        OcclusionQuery query = queries.get(key);
        if (query != null && query.isActive()) {
            query.end();
        }
    }

    /**
     * Updates the PVS tree (e.g. after world changes).
     */
    public CullingEngine withPvs(PvsTree newPvs) {
        return new CullingEngine(gl, newPvs, useOcclusion, usePvs);
    }

    // ---- Statistics ----

    public int testedCount() { return testedCount; }
    public int frustumCulled() { return frustumCulled; }
    public int pvsCulled() { return pvsCulled; }
    public int occlusionCulled() { return occlusionCulled; }
    public int visibleCount() { return visibleCount; }
    public int totalCulled() { return frustumCulled + pvsCulled + occlusionCulled; }

    @Override
    public void close() {
        for (OcclusionQuery q : queries.values()) {
            q.close();
        }
        queries.clear();
    }
}
