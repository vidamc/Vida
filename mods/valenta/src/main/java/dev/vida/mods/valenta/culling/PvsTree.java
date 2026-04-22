/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.culling;

import dev.vida.core.ApiStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Potentially Visible Set (PVS) tree for section-level visibility.
 *
 * <p>Pre-computes which sections are potentially visible from each
 * section using portal-based visibility propagation. At runtime,
 * looking up the PVS for the camera's current section gives a
 * superset of visible sections, reducing the occlusion query load.
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li>For each section, compute portal openings (contiguous transparent
 *       faces on each of the six section boundaries).</li>
 *   <li>Flood-fill through portals up to a configurable depth limit.</li>
 *   <li>Store the result in a compressed bitset per source section.</li>
 * </ol>
 *
 * <h2>Memory</h2>
 * With render distance 16, up to 32³ = 32768 sections; each PVS is a
 * bitset of 32768 bits = 4 KB. Total worst case: 128 MB, but in practice
 * most entries are sparse and the tree prunes aggressively.
 */
@ApiStatus.Preview("valenta")
public final class PvsTree {

    /**
     * Compact boolean-array PVS for one source section.
     */
    public static final class PvsEntry {
        private final long sectionKey;
        private final long[] visibleKeys;

        PvsEntry(long sectionKey, long[] visibleKeys) {
            this.sectionKey = sectionKey;
            this.visibleKeys = visibleKeys;
        }

        public long sectionKey() { return sectionKey; }

        public boolean isVisible(long targetKey) {
            return Arrays.binarySearch(visibleKeys, targetKey) >= 0;
        }

        public int visibleCount() { return visibleKeys.length; }

        public long[] visibleKeys() {
            return Arrays.copyOf(visibleKeys, visibleKeys.length);
        }
    }

    /**
     * Provider of section transparency for PVS computation.
     */
    public interface TransparencyProvider {
        /**
         * @return true if the block at the given section-local coords is transparent
         */
        boolean isTransparent(int sectionX, int sectionY, int sectionZ,
                              int localX, int localY, int localZ);

        /**
         * @return true if the section exists (is loaded)
         */
        boolean sectionExists(int sectionX, int sectionY, int sectionZ);
    }

    private final Map<Long, PvsEntry> entries;
    private final int maxDepth;

    private PvsTree(Map<Long, PvsEntry> entries, int maxDepth) {
        this.entries = entries;
        this.maxDepth = maxDepth;
    }

    /**
     * Looks up PVS for the section containing the camera.
     *
     * @return PVS entry, or null if no data for this section
     */
    @ApiStatus.HotPath
    public PvsEntry lookup(int sectionX, int sectionY, int sectionZ) {
        long key = packKey(sectionX, sectionY, sectionZ);
        return entries.get(key);
    }

    /**
     * Tests if target section is potentially visible from source section.
     *
     * @return true if potentially visible, or true if no PVS data (conservative)
     */
    @ApiStatus.HotPath
    public boolean isPotentiallyVisible(int srcX, int srcY, int srcZ,
                                        int tgtX, int tgtY, int tgtZ) {
        PvsEntry entry = lookup(srcX, srcY, srcZ);
        if (entry == null) return true;
        return entry.isVisible(packKey(tgtX, tgtY, tgtZ));
    }

    public int entryCount() { return entries.size(); }
    public int maxDepth() { return maxDepth; }

    /**
     * Builds PVS for all sections reachable from the given origin sections.
     *
     * @param provider   transparency data source
     * @param sections   set of (sectionX, sectionY, sectionZ) triples
     * @param maxDepth   maximum flood-fill depth (sections)
     * @return compiled PVS tree
     */
    public static PvsTree build(TransparencyProvider provider,
                                Set<long[]> sections, int maxDepth) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(sections, "sections");
        if (maxDepth < 1) throw new IllegalArgumentException("maxDepth < 1");

        Map<Long, PvsEntry> results = new HashMap<>();

        for (long[] sec : sections) {
            int sx = (int) sec[0], sy = (int) sec[1], sz = (int) sec[2];
            long sourceKey = packKey(sx, sy, sz);

            List<Long> visible = new ArrayList<>();
            visible.add(sourceKey);
            floodFill(provider, sx, sy, sz, maxDepth, visible, new HashMap<>());

            long[] keys = visible.stream().mapToLong(Long::longValue).sorted().toArray();
            results.put(sourceKey, new PvsEntry(sourceKey, keys));
        }

        return new PvsTree(Map.copyOf(results), maxDepth);
    }

    /**
     * Creates an empty PVS tree (all sections conservatively visible).
     */
    public static PvsTree empty() {
        return new PvsTree(Map.of(), 0);
    }

    private static void floodFill(TransparencyProvider provider,
                                   int sx, int sy, int sz,
                                   int remaining,
                                   List<Long> visible,
                                   Map<Long, Boolean> visited) {
        if (remaining <= 0) return;

        int[][] neighbors = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int[] dir : neighbors) {
            int nx = sx + dir[0], ny = sy + dir[1], nz = sz + dir[2];
            long key = packKey(nx, ny, nz);
            if (visited.containsKey(key)) continue;
            visited.put(key, Boolean.TRUE);

            if (!provider.sectionExists(nx, ny, nz)) continue;
            if (!hasPortal(provider, sx, sy, sz, dir)) continue;

            visible.add(key);
            floodFill(provider, nx, ny, nz, remaining - 1, visible, visited);
        }
    }

    private static boolean hasPortal(TransparencyProvider provider,
                                      int sx, int sy, int sz, int[] dir) {
        for (int a = 0; a < 16; a++) {
            for (int b = 0; b < 16; b++) {
                int lx, ly, lz;
                if (dir[0] != 0) {
                    lx = dir[0] > 0 ? 15 : 0;
                    ly = a;
                    lz = b;
                } else if (dir[1] != 0) {
                    lx = a;
                    ly = dir[1] > 0 ? 15 : 0;
                    lz = b;
                } else {
                    lx = a;
                    ly = b;
                    lz = dir[2] > 0 ? 15 : 0;
                }
                if (provider.isTransparent(sx, sy, sz, lx, ly, lz)) {
                    return true;
                }
            }
        }
        return false;
    }

    static long packKey(int sx, int sy, int sz) {
        return ((long) sx & 0x1FFFFFL) << 42
                | ((long) sz & 0x1FFFFFL) << 21
                | ((long) sy & 0x1FFFFFL);
    }
}
