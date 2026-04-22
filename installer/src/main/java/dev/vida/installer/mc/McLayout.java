/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.mc;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Канонические пути внутри каталога Minecraft, которые нам нужны, чтобы
 * Vida была видна стандартным лаунчерам.
 *
 * <p>Все пути выражены относительно {@code mcDir} и построены по раскладке,
 * совместимой с официальным Mojang-лаунчером (её же понимают MultiMC / Prism /
 * ATLauncher при импорте профиля).
 */
public final class McLayout {

    /** Maven-группа, под которой Vida регистрирует свою библиотеку. */
    public static final String LIBRARY_GROUP = "dev.vida";
    /** Maven-артефакт loader'а. */
    public static final String LIBRARY_ARTIFACT = "vida-loader";

    private final Path mcDir;
    private final String minecraftVersion;
    private final String loaderVersion;
    private final String profileId;

    public McLayout(Path mcDir, String minecraftVersion, String loaderVersion) {
        this.mcDir = Objects.requireNonNull(mcDir, "mcDir");
        this.minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        this.loaderVersion = Objects.requireNonNull(loaderVersion, "loaderVersion");
        this.profileId = "vida-" + minecraftVersion + "-" + loaderVersion;
    }

    public Path mcDir()                 { return mcDir; }
    public String minecraftVersion()    { return minecraftVersion; }
    public String loaderVersion()       { return loaderVersion; }

    /** Полный идентификатор профиля/версии, например {@code vida-1.21.1-0.5.0}. */
    public String profileId()           { return profileId; }

    /** Относительный путь к loader'у как к Maven-артефакту. */
    public String libraryRelativePath() {
        return LIBRARY_GROUP.replace('.', '/')
                + "/" + LIBRARY_ARTIFACT
                + "/" + loaderVersion
                + "/" + LIBRARY_ARTIFACT + "-" + loaderVersion + ".jar";
    }

    /** Абсолютный путь к loader.jar в {@code libraries/...}. */
    public Path libraryJar() {
        return mcDir.resolve("libraries").resolve(libraryRelativePath());
    }

    /** Каталог {@code versions/vida-1.21.1-0.5.0/}. */
    public Path versionDir() {
        return mcDir.resolve("versions").resolve(profileId);
    }

    /** Version-JSON: {@code versions/<id>/<id>.json}. */
    public Path versionJson() {
        return versionDir().resolve(profileId + ".json");
    }

    /** Пустой jar-маркер: {@code versions/<id>/<id>.jar}. */
    public Path versionJar() {
        return versionDir().resolve(profileId + ".jar");
    }

    /** Файл профилей для Mojang-лаунчера. */
    public Path launcherProfiles() {
        return mcDir.resolve("launcher_profiles.json");
    }

    /** Общий каталог модов. */
    public Path modsDir() {
        return mcDir.resolve("mods");
    }

    /** Наш служебный корень — конфиги/логи/audit. */
    public Path vidaDir() {
        return mcDir.resolve("vida");
    }

    public Path vidaConfigDir() { return vidaDir().resolve("config"); }
    public Path vidaLogsDir()   { return vidaDir().resolve("logs"); }
    public Path vidaInstallJson() { return vidaDir().resolve("install.json"); }

    /** Human-readable имя профиля, отображаемое в лаунчере. */
    public String displayName() {
        return "Vida " + minecraftVersion + " (" + loaderVersion + ")";
    }

    /** Maven-координата loader'а в GAV-форме, для поля {@code libraries.name}. */
    public String loaderMavenCoord() {
        return LIBRARY_GROUP + ":" + LIBRARY_ARTIFACT + ":" + loaderVersion;
    }
}
