/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.chunk;

import dev.vida.core.ApiStatus;
import dev.vida.mods.valenta.core.BiomeBlendSsbo;
import dev.vida.mods.valenta.core.BlockLightSsbo;
import dev.vida.mods.valenta.core.VboMallaBatcher;
import java.util.List;
import java.util.Objects;

/**
 * Upload stage: runs on the render thread to transfer completed meshes to GPU.
 *
 * <p>Consumes {@link MallaChunk} results from the build stage and feeds
 * them into the {@link VboMallaBatcher} and SSBO layers. After all
 * sections are uploaded, the caller invokes
 * {@link VboMallaBatcher#flush()} for the actual draw.
 *
 * <h2>Performance budget</h2>
 * The upload stage targets &lt; 1 ms per frame. If too many sections are
 * ready simultaneously, only the first {@code maxUploadsPerFrame} are
 * processed; the rest are deferred to the next frame.
 */
@ApiStatus.Preview("valenta")
public final class UploadEtapa {

    private final VboMallaBatcher batcher;
    private final BiomeBlendSsbo biomeBlend;
    private final BlockLightSsbo blockLight;
    private final int maxUploadsPerFrame;
    private int uploadedThisFrame;

    /**
     * @param batcher            the VBO batcher to fill
     * @param biomeBlend         biome blend SSBO (nullable if disabled)
     * @param blockLight         block light SSBO (nullable if disabled)
     * @param maxUploadsPerFrame max sections to upload per frame
     */
    public UploadEtapa(VboMallaBatcher batcher,
                       BiomeBlendSsbo biomeBlend,
                       BlockLightSsbo blockLight,
                       int maxUploadsPerFrame) {
        this.batcher = Objects.requireNonNull(batcher, "batcher");
        this.biomeBlend = biomeBlend;
        this.blockLight = blockLight;
        if (maxUploadsPerFrame < 1) {
            throw new IllegalArgumentException("maxUploadsPerFrame < 1");
        }
        this.maxUploadsPerFrame = maxUploadsPerFrame;
    }

    /**
     * Prepares for a new frame. Must be called before any uploads.
     */
    @ApiStatus.HotPath
    public void beginFrame() {
        batcher.beginFrame();
        if (biomeBlend != null) biomeBlend.beginFrame();
        if (blockLight != null) blockLight.beginFrame();
        uploadedThisFrame = 0;
    }

    /**
     * Uploads completed meshes to GPU buffers.
     *
     * @param completedMeshes meshes ready for upload
     * @return number of meshes actually uploaded (may be &lt; input size due to cap)
     */
    @ApiStatus.HotPath
    public int subir(List<MallaChunk> completedMeshes) {
        Objects.requireNonNull(completedMeshes, "completedMeshes");
        int uploaded = 0;
        for (MallaChunk malla : completedMeshes) {
            if (uploadedThisFrame >= maxUploadsPerFrame) break;

            int sectionId = (int) (malla.sectionKey() & 0xFFFFFFFFL);
            batcher.appendSection(
                    malla.vertices(), malla.indices(),
                    malla.vertexCount(), malla.indexCount(),
                    sectionId);

            if (biomeBlend != null && malla.biomeBlend() != null) {
                biomeBlend.appendSection(malla.biomeBlend());
            }
            if (blockLight != null && malla.lightData() != null) {
                blockLight.appendSection(malla.lightData());
            }

            uploadedThisFrame++;
            uploaded++;
        }
        return uploaded;
    }

    /**
     * Finalizes uploads: pushes SSBOs to GPU.
     */
    @ApiStatus.HotPath
    public void finalizarUploads() {
        if (biomeBlend != null) biomeBlend.upload();
        if (blockLight != null) blockLight.upload();
    }

    public int uploadedThisFrame() { return uploadedThisFrame; }
    public int maxUploadsPerFrame() { return maxUploadsPerFrame; }
}
