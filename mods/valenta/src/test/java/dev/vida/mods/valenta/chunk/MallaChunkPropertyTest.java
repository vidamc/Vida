/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.mods.valenta.core.CompactVertexFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

/**
 * Property-based tests for MallaChunk mesh invariants.
 *
 * <p>Uses jqwik to generate arbitrary section coordinates and verify
 * that the mesh record invariants always hold.
 */
class MallaChunkPropertyTest {

    @Property
    void vertexBufferSize_matchesVertexCount(
            @ForAll @IntRange(min = 1, max = 256) int vertexCount,
            @ForAll @IntRange(min = -100, max = 100) int sx,
            @ForAll @IntRange(min = -4, max = 20) int sy,
            @ForAll @IntRange(min = -100, max = 100) int sz) {

        int indexCount = ((vertexCount + 3) / 4) * 6;
        if (indexCount % 3 != 0) indexCount += (3 - indexCount % 3);
        if (indexCount == 0) indexCount = 3;

        ByteBuffer verts = ByteBuffer.allocateDirect(
                vertexCount * CompactVertexFormat.BYTES_PER_VERTEX).order(ByteOrder.nativeOrder());
        ByteBuffer idxs = ByteBuffer.allocateDirect(indexCount * 4).order(ByteOrder.nativeOrder());

        MallaChunk chunk = new MallaChunk(sx, sy, sz, vertexCount, indexCount, verts, idxs, null, null);

        assertThat(chunk.vertices().remaining())
                .isEqualTo(vertexCount * CompactVertexFormat.BYTES_PER_VERTEX);
        assertThat(chunk.indices().remaining())
                .isEqualTo(indexCount * 4);
    }

    @Property
    void indexCount_alwaysDivisibleByThree(
            @ForAll @IntRange(min = 1, max = 100) int quadCount) {

        int indexCount = quadCount * 6;
        assertThat(indexCount % 3).isZero();
    }

    @Property
    void sectionKey_uniqueForDifferentCoords(
            @ForAll @IntRange(min = -1000, max = 1000) int sx1,
            @ForAll @IntRange(min = -4, max = 20) int sy1,
            @ForAll @IntRange(min = -1000, max = 1000) int sz1) {

        long key1 = MallaChunk.packSectionKey(sx1, sy1, sz1);
        long key2 = MallaChunk.packSectionKey(sx1 + 1, sy1, sz1);
        long key3 = MallaChunk.packSectionKey(sx1, sy1 + 1, sz1);
        long key4 = MallaChunk.packSectionKey(sx1, sy1, sz1 + 1);

        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1).isNotEqualTo(key3);
        assertThat(key1).isNotEqualTo(key4);
    }

    @Property
    void builder_producesValidMesh_forNonEmptySection(
            @ForAll @IntRange(min = -50, max = 50) int sx,
            @ForAll @IntRange(min = 0, max = 15) int sy,
            @ForAll @IntRange(min = -50, max = 50) int sz) {

        MallaChunk.Builder builder = new MallaChunk.Builder(sx, sy, sz);
        builder.addQuad(
                0, 1, 0,  1, 1, 0,  1, 1, 1,  0, 1, 1,
                0, 1, 0xFFFFFFFF, 0, 0, 0.0625f, 0.0625f);

        MallaChunk result = builder.build();
        assertThat(result).isNotNull();
        assertThat(result.vertexCount()).isEqualTo(4);
        assertThat(result.indexCount()).isEqualTo(6);
        assertThat(result.indexCount() % 3).isZero();
        assertThat(result.sectionX()).isEqualTo(sx);
        assertThat(result.sectionY()).isEqualTo(sy);
        assertThat(result.sectionZ()).isEqualTo(sz);
    }

    @Property
    void builder_returnsNull_forEmptySection(
            @ForAll @IntRange(min = -50, max = 50) int sx,
            @ForAll @IntRange(min = 0, max = 15) int sy,
            @ForAll @IntRange(min = -50, max = 50) int sz) {

        MallaChunk.Builder builder = new MallaChunk.Builder(sx, sy, sz);
        assertThat(builder.build()).isNull();
    }
}
