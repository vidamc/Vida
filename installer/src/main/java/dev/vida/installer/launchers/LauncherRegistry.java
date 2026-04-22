/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers;

import dev.vida.installer.launchers.atlauncher.ATLauncherHandler;
import dev.vida.installer.launchers.curseforge.CurseForgeHandler;
import dev.vida.installer.launchers.modrinth.ModrinthHandler;
import dev.vida.installer.launchers.mojang.MojangHandler;
import dev.vida.installer.launchers.multimc.MultiMCHandler;
import dev.vida.installer.launchers.prism.PrismHandler;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Реестр {@link LauncherHandler}'ов.
 *
 * <p>Singleton-like: возвращает одну и ту же реализацию для каждого
 * {@link LauncherKind}. Handler'ы stateless.
 */
public final class LauncherRegistry {

    private static final Map<LauncherKind, LauncherHandler> HANDLERS;

    static {
        Map<LauncherKind, LauncherHandler> m = new EnumMap<>(LauncherKind.class);
        m.put(LauncherKind.MOJANG,      new MojangHandler());
        m.put(LauncherKind.PRISM,       new PrismHandler());
        m.put(LauncherKind.MULTIMC,     new MultiMCHandler());
        m.put(LauncherKind.ATLAUNCHER,  new ATLauncherHandler());
        m.put(LauncherKind.MODRINTH,    new ModrinthHandler());
        m.put(LauncherKind.CURSEFORGE,  new CurseForgeHandler());
        HANDLERS = Map.copyOf(m);
    }

    private LauncherRegistry() {}

    /** Возвращает handler для указанного лаунчера. */
    public static LauncherHandler forKind(LauncherKind kind) {
        LauncherHandler h = HANDLERS.get(kind);
        if (h == null) {
            throw new IllegalArgumentException("Launcher not yet implemented: "
                    + kind.displayName() + " (will be added in a later phase)");
        }
        return h;
    }

    /** Все реализованные handler'ы. */
    public static List<LauncherHandler> all() {
        return List.copyOf(HANDLERS.values());
    }

    /** {@code true} если kind имеет реализацию (против {@link LauncherKind#isImplemented()}). */
    public static boolean isAvailable(LauncherKind kind) {
        return HANDLERS.containsKey(kind);
    }
}
