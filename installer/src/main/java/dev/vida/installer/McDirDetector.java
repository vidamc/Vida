/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Определение стандартного каталога Minecraft на текущей ОС.
 *
 * <p>Поведение:
 * <ul>
 *   <li>Windows → {@code %APPDATA%/.minecraft};</li>
 *   <li>macOS   → {@code ~/Library/Application Support/minecraft};</li>
 *   <li>иначе  → {@code ~/.minecraft}.</li>
 * </ul>
 *
 * <p>Существование каталога не проверяется — возвращается каноническое
 * место, где он был бы, если бы Minecraft был установлен.
 */
public final class McDirDetector {

    private final String os;
    private final String userHome;
    private final String appData;

    /** Системный детектор: читает {@code os.name}, {@code user.home}, {@code APPDATA}. */
    public McDirDetector() {
        this(System.getProperty("os.name", ""),
             System.getProperty("user.home", "."),
             System.getenv("APPDATA"));
    }

    /** Для тестов — все входы явные. */
    public McDirDetector(String os, String userHome, String appData) {
        this.os       = os == null ? "" : os;
        this.userHome = userHome == null ? "." : userHome;
        this.appData  = appData;
    }

    public Path defaultDir() {
        String osLower = os.toLowerCase(Locale.ROOT);
        if (osLower.contains("win")) {
            String base = appData != null && !appData.isBlank() ? appData : userHome;
            return Paths.get(base, ".minecraft");
        }
        if (osLower.contains("mac") || osLower.contains("darwin")) {
            return Paths.get(userHome, "Library", "Application Support", "minecraft");
        }
        return Paths.get(userHome, ".minecraft");
    }
}
