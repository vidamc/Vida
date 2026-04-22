package net.minecraft.client.renderer;

import java.util.Set;

public class LevelRenderer {
    public Object viewArea;
    public Set<Object> renderChunks;
    public Set<Object> globalBlockEntities;

    public void renderLevel(Object poseStack, float partialTick, long nanoTime,
                            boolean renderBlockOutline, Object camera,
                            Object gameRenderer, Object lightTexture, Object projectionMatrix) {}

    public void setupRender(Object camera, Object frustum, boolean hasCapturedFrustum,
                            boolean spectator) {}

    public void renderClouds(Object poseStack, Object projectionMatrix, float partialTick,
                             double camX, double camY, double camZ) {}
}
