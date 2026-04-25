/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.internal;

import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs {@code entrypoints.client} once the Minecraft client thread has entered
 * {@code Minecraft.tick()} — after GLFW and the GL context exist. Premain-time
 * {@code iniciar()} must not touch LWJGL/OpenGL.
 */
@ApiStatus.Internal
public final class ClientEntrypointScheduler {

    private static final Log LOG = Log.of(ClientEntrypointScheduler.class);

    private static final CopyOnWriteArrayList<Runnable> PENDING = new CopyOnWriteArrayList<>();
    private static final AtomicBoolean FLUSHED = new AtomicBoolean(false);

    private ClientEntrypointScheduler() {}

    /** Clears any leftover tasks and allows a subsequent boot (tests / dev). */
    public static void resetForNewBootSession() {
        PENDING.clear();
        FLUSHED.set(false);
    }

    public static void enqueue(Runnable task) {
        if (task != null) {
            PENDING.add(task);
        }
    }

    /**
     * Invoked from {@link dev.vida.platform.VanillaBridge#onClientTick()} before
     * {@code LatidoPulso} dispatch.
     */
    public static void flushPendingOnce() {
        if (!FLUSHED.compareAndSet(false, true)) {
            return;
        }
        List<Runnable> batch = List.copyOf(PENDING);
        PENDING.clear();
        for (Runnable r : batch) {
            try {
                r.run();
            } catch (Throwable t) {
                LOG.warn("Vida: deferred client entrypoint failed: {}", t.toString());
            }
        }
    }
}
