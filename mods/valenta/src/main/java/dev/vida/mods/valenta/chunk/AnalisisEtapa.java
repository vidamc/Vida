/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.chunk;

import dev.vida.core.ApiStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Analysis stage of the chunk meshing pipeline.
 *
 * <p>Determines which sections need (re-)meshing based on dirty flags
 * and camera distance, then produces a sorted list of
 * {@link SectionRequest} ordered by priority (closest-first).
 *
 * <p>Runs on the render thread as a quick scan; the actual meshing
 * happens in {@link BuildEtapa} on worker threads.
 */
@ApiStatus.Preview("valenta")
public final class AnalisisEtapa {

    /**
     * A request to mesh one section, sorted by squared distance from camera.
     *
     * @param sectionX    section coordinate X
     * @param sectionY    section coordinate Y
     * @param sectionZ    section coordinate Z
     * @param distanceSq  squared distance from camera to section center
     * @param priority    higher value = higher priority (used for tie-breaking)
     */
    public record SectionRequest(int sectionX, int sectionY, int sectionZ,
                                 double distanceSq, int priority)
            implements Comparable<SectionRequest> {

        @Override
        public int compareTo(SectionRequest o) {
            int c = Integer.compare(o.priority, this.priority);
            if (c != 0) return c;
            return Double.compare(this.distanceSq, o.distanceSq);
        }
    }

    private final int maxRequestsPerFrame;

    /**
     * @param maxRequestsPerFrame cap on how many sections can be scheduled per frame
     */
    public AnalisisEtapa(int maxRequestsPerFrame) {
        if (maxRequestsPerFrame < 1) {
            throw new IllegalArgumentException("maxRequestsPerFrame < 1");
        }
        this.maxRequestsPerFrame = maxRequestsPerFrame;
    }

    /**
     * Scans dirty sections and returns a priority-sorted list of rebuild requests.
     *
     * @param dirtySections coordinates of sections with dirty flags set
     * @param cameraX       camera world X
     * @param cameraY       camera world Y
     * @param cameraZ       camera world Z
     * @param renderDistance render distance in sections
     * @return sorted list of requests (highest priority first), capped at max
     */
    public List<SectionRequest> analizar(List<int[]> dirtySections,
                                         double cameraX, double cameraY, double cameraZ,
                                         int renderDistance) {
        Objects.requireNonNull(dirtySections, "dirtySections");
        if (dirtySections.isEmpty()) return List.of();

        double rdSq = (double) renderDistance * 16 * renderDistance * 16;
        List<SectionRequest> requests = new ArrayList<>(
                Math.min(dirtySections.size(), maxRequestsPerFrame));

        for (int[] sec : dirtySections) {
            double cx = (sec[0] * 16) + 8.0;
            double cy = (sec[1] * 16) + 8.0;
            double cz = (sec[2] * 16) + 8.0;
            double dx = cameraX - cx;
            double dy = cameraY - cy;
            double dz = cameraZ - cz;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > rdSq) continue;

            int prio = distSq < 64 * 64 ? 2 : (distSq < 256 * 256 ? 1 : 0);
            requests.add(new SectionRequest(sec[0], sec[1], sec[2], distSq, prio));
        }

        Collections.sort(requests);
        if (requests.size() > maxRequestsPerFrame) {
            return List.copyOf(requests.subList(0, maxRequestsPerFrame));
        }
        return List.copyOf(requests);
    }

    public int maxRequestsPerFrame() {
        return maxRequestsPerFrame;
    }
}
