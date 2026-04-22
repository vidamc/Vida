/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer;

import dev.vida.installer.launchers.LauncherKind;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Параметры одной инсталляции.
 *
 * @param launcherKind          целевой лаунчер. По умолчанию {@link LauncherKind#MOJANG}.
 * @param installDir            корневой каталог лаунчера. Смысл различается по {@code launcherKind}:
 *                              <ul>
 *                                <li>{@link LauncherKind#MOJANG} — {@code .minecraft};</li>
 *                                <li>{@link LauncherKind#PRISM}/{@link LauncherKind#MULTIMC} — data-dir
 *                                    лаунчера (содержит {@code instances/});</li>
 *                                <li>{@link LauncherKind#ATLAUNCHER} — BASE_DIR
 *                                    лаунчера (содержит {@code instances/});</li>
 *                                <li>Modrinth/CurseForge — аналогично.</li>
 *                              </ul>
 * @param targetInstance        для режима «patch existing» — путь к конкретному
 *                              instance'у внутри {@code installDir}. {@code null} для
 *                              режима «create new».
 * @param instanceName          желаемое имя нового instance'а (для лаунчеров, поддерживающих
 *                              создание). {@code null} → дефолтное имя вида «Vida 1.21.1».
 * @param minecraftVersion      версия Minecraft (идёт в {@code inheritsFrom})
 * @param loaderVersion         версия loader'а (идёт в Maven-координаты)
 * @param createLauncherProfile добавлять ли запись в {@code launcher_profiles.json}
 *                              (только для {@link LauncherKind#MOJANG})
 * @param createLaunchScript    писать ли standalone {@code vida.bat}/{@code vida.sh}
 *                              (только для {@link LauncherKind#MOJANG})
 * @param dryRun                если {@code true} — не пишем в ФС, только возвращаем репорт
 * @param overwrite             перезаписывать ли существующую установку
 */
public record InstallOptions(
        LauncherKind launcherKind,
        Path installDir,
        Path targetInstance,
        String instanceName,
        String minecraftVersion,
        String loaderVersion,
        boolean createLauncherProfile,
        boolean createLaunchScript,
        boolean dryRun,
        boolean overwrite) {

    public InstallOptions {
        Objects.requireNonNull(launcherKind, "launcherKind");
        Objects.requireNonNull(installDir, "installDir");
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        Objects.requireNonNull(loaderVersion, "loaderVersion");
        // targetInstance, instanceName — nullable
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private LauncherKind launcherKind = LauncherKind.MOJANG;
        private Path installDir;
        private Path targetInstance;
        private String instanceName;
        private String minecraftVersion = "1.21.1";
        private String loaderVersion = "unknown";
        private boolean createLauncherProfile = true;
        private boolean createLaunchScript = true;
        private boolean dryRun = false;
        private boolean overwrite = false;

        public Builder launcherKind(LauncherKind v)      { this.launcherKind = v; return this; }
        public Builder installDir(Path p)                { this.installDir = p; return this; }
        public Builder targetInstance(Path p)            { this.targetInstance = p; return this; }
        public Builder instanceName(String s)            { this.instanceName = s; return this; }
        public Builder minecraftVersion(String v)        { this.minecraftVersion = v; return this; }
        public Builder loaderVersion(String v)           { this.loaderVersion = v; return this; }
        public Builder createLauncherProfile(boolean v)  { this.createLauncherProfile = v; return this; }
        public Builder createLaunchScript(boolean v)     { this.createLaunchScript = v; return this; }
        public Builder dryRun(boolean v)                 { this.dryRun = v; return this; }
        public Builder overwrite(boolean v)              { this.overwrite = v; return this; }

        public InstallOptions build() {
            return new InstallOptions(launcherKind, installDir, targetInstance, instanceName,
                    minecraftVersion, loaderVersion,
                    createLauncherProfile, createLaunchScript, dryRun, overwrite);
        }
    }
}
