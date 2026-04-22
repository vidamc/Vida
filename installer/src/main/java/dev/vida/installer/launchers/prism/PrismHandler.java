/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.prism;

import dev.vida.installer.launchers.LauncherKind;
import dev.vida.installer.launchers.OsPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler для Prism Launcher. Использует component {@code +agents} для
 * инъекции {@code -javaagent} (поддерживается Prism ≥ 7.0).
 */
public final class PrismHandler extends PrismLikeHandler {

    @Override
    public LauncherKind kind() { return LauncherKind.PRISM; }

    @Override
    protected boolean supportsAgents() { return true; }

    @Override
    public List<Path> detectDataDirs() {
        OsPaths os = OsPaths.system();
        List<Path> out = new ArrayList<>();
        addIfExists(out, os.prism());
        addIfExists(out, os.prismFlatpak());
        return out;
    }

    private static void addIfExists(List<Path> acc, Path p) {
        if (p != null && Files.isDirectory(p)) acc.add(p);
    }
}
