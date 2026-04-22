/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.culling;

import dev.vida.core.ApiStatus;
import dev.vida.mods.valenta.core.GlFunctions;
import java.util.Objects;

/**
 * Wrapper around a GL occlusion query object for one chunk section.
 *
 * <p>Usage pattern:
 * <ol>
 *   <li>Call {@link #begin()} before rendering the section's bounding box.</li>
 *   <li>Render a simplified proxy geometry (e.g. solid AABB).</li>
 *   <li>Call {@link #end()} after the proxy.</li>
 *   <li>On the next frame, call {@link #isVisible()} to read back the result.</li>
 * </ol>
 *
 * <p>Results are always one frame delayed to avoid GPU stalls. Sections
 * that were visible last frame are rendered unconditionally; newly visible
 * sections get a conditional render via {@code glBeginConditionalRender}.
 */
@ApiStatus.Preview("valenta")
public final class OcclusionQuery implements AutoCloseable {

    private static final int GL_SAMPLES_PASSED = 0x8914;
    private static final int GL_QUERY_RESULT = 0x8866;
    private static final int GL_QUERY_BY_REGION_WAIT = 0x8E15;

    private final GlFunctions gl;
    private int queryId;
    private boolean active;
    private boolean hasResult;
    private boolean lastVisible = true;
    private boolean closed;

    public OcclusionQuery(GlFunctions gl) {
        this.gl = Objects.requireNonNull(gl, "gl");
        this.queryId = gl.glGenQueries();
    }

    /**
     * Begins the occlusion query.
     */
    public void begin() {
        ensureOpen();
        if (active) {
            throw new IllegalStateException("query already active");
        }
        gl.glBeginQuery(GL_SAMPLES_PASSED, queryId);
        active = true;
    }

    /**
     * Ends the occlusion query.
     */
    public void end() {
        ensureOpen();
        if (!active) {
            throw new IllegalStateException("query not active");
        }
        gl.glEndQuery(GL_SAMPLES_PASSED);
        active = false;
        hasResult = true;
    }

    /**
     * Reads the query result. Always returns the result from the previous frame's
     * query to avoid stalls. On the first frame, defaults to visible.
     *
     * @return true if any samples passed (section is visible)
     */
    @ApiStatus.HotPath
    public boolean isVisible() {
        ensureOpen();
        if (!hasResult) return lastVisible;
        int samples = gl.glGetQueryObjectiv(queryId, GL_QUERY_RESULT);
        lastVisible = samples > 0;
        hasResult = false;
        return lastVisible;
    }

    /**
     * Begins conditional rendering: the GPU skips draw calls if the query
     * determined the section is occluded.
     */
    public void beginConditionalRender() {
        ensureOpen();
        gl.glBeginConditionalRender(queryId, GL_QUERY_BY_REGION_WAIT);
    }

    /**
     * Ends conditional rendering.
     */
    public void endConditionalRender() {
        ensureOpen();
        gl.glEndConditionalRender();
    }

    public boolean isActive() { return active; }

    @Override
    public void close() {
        if (!closed) {
            if (active) {
                gl.glEndQuery(GL_SAMPLES_PASSED);
            }
            gl.glDeleteQueries(queryId);
            queryId = 0;
            closed = true;
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("OcclusionQuery already closed");
    }
}
