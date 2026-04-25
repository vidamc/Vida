/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.prism;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Канонические пути внутри одного Prism/MultiMC instance'а.
 *
 * <p>Формат identichen у Prism Launcher и MultiMC:
 * <pre>
 * &lt;prism-data&gt;/instances/&lt;name&gt;/
 *   instance.cfg            ← INI, per-instance settings
 *   mmc-pack.json           ← component list
 *   patches/
 *     dev.vida.loader.json  ← наш компонент (с +agents для Prism)
 *   libraries/
 *     loader-&lt;ver&gt;.jar      ← при MMC-hint: local
 *   minecraft/              ← game dir (как в Prism/MultiMC gameRoot())
 *     mods/
 *   vida/
 *     install.json          ← audit-inventory
 * </pre>
 */
public final class PrismInstanceLayout {

    /** UID нашего компонента в component-list'е mmc-pack.json. */
    public static final String COMPONENT_UID = "dev.vida.loader";

    /** Имя компонента, отображаемое в GUI лаунчера. */
    public static final String COMPONENT_NAME = "Vida Loader";

    /**
     * Имя artifact в имени файла loader-&lt;ver&gt;.jar, когда используется
     * {@code MMC-hint: "local"}. Launcher читает его по шаблону
     * {@code &lt;artifact&gt;-&lt;version&gt;.jar}.
     */
    public static final String LOCAL_ARTIFACT = "loader";

    /** Maven-группа для поля {@code libraries.name} в component patch. */
    public static final String LIBRARY_GROUP = "dev.vida";

    private final Path dataDir;
    private final Path instanceDir;
    private final String instanceName;
    private final String minecraftVersion;
    private final String loaderVersion;

    public PrismInstanceLayout(Path dataDir,
                               String instanceName,
                               String minecraftVersion,
                               String loaderVersion) {
        this.dataDir = Objects.requireNonNull(dataDir, "dataDir").toAbsolutePath().normalize();
        this.instanceName = Objects.requireNonNull(instanceName, "instanceName");
        this.minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        this.loaderVersion = Objects.requireNonNull(loaderVersion, "loaderVersion");
        this.instanceDir = this.dataDir.resolve("instances").resolve(instanceName);
    }

    public Path dataDir()            { return dataDir; }
    public Path instancesDir()       { return dataDir.resolve("instances"); }
    public Path instanceDir()        { return instanceDir; }
    public String instanceName()     { return instanceName; }
    public String minecraftVersion() { return minecraftVersion; }
    public String loaderVersion()    { return loaderVersion; }

    public Path instanceCfg()        { return instanceDir.resolve("instance.cfg"); }
    public Path mmcPackJson()        { return instanceDir.resolve("mmc-pack.json"); }
    public Path patchesDir()         { return instanceDir.resolve("patches"); }
    public Path componentPatch()     { return patchesDir().resolve(COMPONENT_UID + ".json"); }

    public Path librariesDir()       { return instanceDir.resolve("libraries"); }
    public Path loaderJar() {
        return librariesDir().resolve(LOCAL_ARTIFACT + "-" + loaderVersion + ".jar");
    }

    /**
     * Game-directory. Совпадает с {@code MinecraftInstance::gameRoot()} в Prism:
     * приоритетно {@code minecraft/} (см. MultiMC wiki Folder Structure).
     */
    public Path gameDir()            { return instanceDir.resolve("minecraft"); }
    public Path modsDir()            { return gameDir().resolve("mods"); }

    public Path vidaDir()            { return instanceDir.resolve("vida"); }
    public Path vidaInstallJson()    { return vidaDir().resolve("install.json"); }

    /** Maven GAV для поля {@code libraries.name} в component patch. */
    public String libraryMavenCoord() {
        return LIBRARY_GROUP + ":" + LOCAL_ARTIFACT + ":" + loaderVersion;
    }

    /** Отображаемое имя instance'а в GUI лаунчера. */
    public String displayInstanceName() {
        return "Vida " + minecraftVersion + " (" + loaderVersion + ")";
    }

    // ------------------------------------------------------------------
    //  safe-name generation
    // ------------------------------------------------------------------

    /**
     * Генерирует безопасное имя каталога из предложенного отображаемого имени.
     * Prism/MultiMC принимают почти любые имена, но space-safe путь удобнее
     * для диагностики и Windows-quoting.
     */
    public static String sanitizeInstanceName(String proposed) {
        if (proposed == null || proposed.isBlank()) {
            return "vida-instance";
        }
        StringBuilder sb = new StringBuilder(proposed.length());
        for (int i = 0; i < proposed.length(); i++) {
            char c = proposed.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.') {
                sb.append(c);
            } else if (c == ' ') {
                sb.append('-');
            }
            // всё прочее отбрасываем
        }
        String s = sb.toString();
        while (s.startsWith(".")) s = s.substring(1);
        return s.isEmpty() ? "vida-instance" : s;
    }

    /** Default-имя для новой Vida-инсталляции. */
    public static String defaultInstanceName(String minecraftVersion, String loaderVersion) {
        return sanitizeInstanceName("vida-" + minecraftVersion + "-" + loaderVersion);
    }
}
