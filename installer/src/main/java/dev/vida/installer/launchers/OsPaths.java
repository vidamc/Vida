/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Унифицированное определение путей для разных лаунчеров.
 *
 * <p>Читает системные свойства {@code os.name}, {@code user.home} и
 * переменные окружения {@code APPDATA}, {@code LOCALAPPDATA},
 * {@code XDG_DATA_HOME}, {@code XDG_CONFIG_HOME}. Для тестов все входы
 * передаются явно в {@link #of(String, String, String, String, String, String)}.
 *
 * <p>Каждый метод возвращает <em>канонический</em> путь (как если бы
 * соответствующий лаунчер был установлен в стандартное место); существование
 * каталога на диске не проверяется — это задача handler'ов.
 */
public final class OsPaths {

    public enum Family { WINDOWS, MACOS, LINUX }

    private final Family family;
    private final String userHome;
    private final String appData;       // %APPDATA% (roaming)
    private final String localAppData;  // %LOCALAPPDATA%
    private final String xdgData;       // $XDG_DATA_HOME
    private final String xdgConfig;     // $XDG_CONFIG_HOME

    OsPaths(Family family,
            String userHome,
            String appData,
            String localAppData,
            String xdgData,
            String xdgConfig) {
        this.family = family;
        this.userHome = userHome == null || userHome.isBlank() ? "." : userHome;
        this.appData = appData;
        this.localAppData = localAppData;
        this.xdgData = xdgData;
        this.xdgConfig = xdgConfig;
    }

    /** Системный конструктор — берёт данные из JVM и env. */
    public static OsPaths system() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Family f;
        if (os.contains("win"))                            f = Family.WINDOWS;
        else if (os.contains("mac") || os.contains("darwin")) f = Family.MACOS;
        else                                                f = Family.LINUX;
        return new OsPaths(f,
                System.getProperty("user.home"),
                System.getenv("APPDATA"),
                System.getenv("LOCALAPPDATA"),
                System.getenv("XDG_DATA_HOME"),
                System.getenv("XDG_CONFIG_HOME"));
    }

    /** Явный конструктор для тестов. */
    public static OsPaths of(String osName,
                             String userHome,
                             String appData,
                             String localAppData,
                             String xdgData,
                             String xdgConfig) {
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        Family f;
        if (os.contains("win"))                            f = Family.WINDOWS;
        else if (os.contains("mac") || os.contains("darwin")) f = Family.MACOS;
        else                                                f = Family.LINUX;
        return new OsPaths(f, userHome, appData, localAppData, xdgData, xdgConfig);
    }

    public Family family() { return family; }

    public boolean isWindows() { return family == Family.WINDOWS; }
    public boolean isMac()     { return family == Family.MACOS; }
    public boolean isLinux()   { return family == Family.LINUX; }

    /** {@code %APPDATA%} (Roaming) на Windows, иначе null. */
    public String appData() { return nullOrBlank(appData) ? null : appData; }

    /** {@code %LOCALAPPDATA%} на Windows, иначе null. */
    public String localAppData() { return nullOrBlank(localAppData) ? null : localAppData; }

    /** Home-директория пользователя. */
    public Path home() { return Paths.get(userHome); }

    // ----------------------------------------------------------- Mojang

    /**
     * Каталог {@code .minecraft}/{@code minecraft}.
     *
     * <ul>
     *   <li>Win: {@code %APPDATA%/.minecraft}</li>
     *   <li>macOS: {@code ~/Library/Application Support/minecraft}</li>
     *   <li>Linux: {@code ~/.minecraft}</li>
     * </ul>
     */
    public Path minecraft() {
        return switch (family) {
            case WINDOWS -> Paths.get(nullOrBlank(appData) ? userHome : appData, ".minecraft");
            case MACOS   -> Paths.get(userHome, "Library", "Application Support", "minecraft");
            case LINUX   -> Paths.get(userHome, ".minecraft");
        };
    }

    // ----------------------------------------------------------- Prism

    /**
     * Стандартный data-dir Prism Launcher.
     *
     * <ul>
     *   <li>Win: {@code %APPDATA%/PrismLauncher}</li>
     *   <li>macOS: {@code ~/Library/Application Support/PrismLauncher}</li>
     *   <li>Linux: {@code ~/.local/share/PrismLauncher} (или {@code $XDG_DATA_HOME/PrismLauncher})</li>
     * </ul>
     *
     * <p>Также проверьте Flatpak-вариант через {@link #prismFlatpak()}.
     */
    public Path prism() {
        return switch (family) {
            case WINDOWS -> Paths.get(nullOrBlank(appData) ? userHome : appData, "PrismLauncher");
            case MACOS   -> Paths.get(userHome, "Library", "Application Support", "PrismLauncher");
            case LINUX   -> xdgDataOr(Paths.get(userHome, ".local", "share"), "PrismLauncher");
        };
    }

    /** Linux-Flatpak вариант Prism: {@code ~/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher}. */
    public Path prismFlatpak() {
        return Paths.get(userHome, ".var", "app", "org.prismlauncher.PrismLauncher",
                "data", "PrismLauncher");
    }

    // ----------------------------------------------------------- ATLauncher

    /**
     * Вероятные кандидаты ATLauncher base-dir'а. ATLauncher — portable-by-design,
     * реальная BASE_DIR = {@code user.dir} лаунчера, который нам из отдельного
     * процесса недоступен. Возвращаем наиболее частые места установки.
     */
    public Path[] atlauncherCandidates() {
        return switch (family) {
            case WINDOWS -> new Path[]{
                    Paths.get(userHome, "ATLauncher"),
                    nullOrBlank(appData) ? Paths.get(userHome, ".atlauncher")
                                         : Paths.get(appData, ".atlauncher"),
            };
            case MACOS -> new Path[]{
                    Paths.get(userHome, "Library", "Application Support", "ATLauncher"),
                    Paths.get(userHome, ".atlauncher"),
            };
            case LINUX -> new Path[]{
                    xdgDataOr(Paths.get(userHome, ".local", "share"), "ATLauncher"),
                    Paths.get(userHome, "ATLauncher"),
                    Paths.get(userHome, ".atlauncher"),
            };
        };
    }

    // ----------------------------------------------------------- Modrinth (Phase B)

    /** Стандартный data-dir Modrinth App. */
    public Path modrinth() {
        return switch (family) {
            case WINDOWS -> Paths.get(nullOrBlank(appData) ? userHome : appData,
                    "com.modrinth.ModrinthApp");
            case MACOS   -> Paths.get(userHome, "Library", "Application Support",
                    "com.modrinth.ModrinthApp");
            case LINUX   -> xdgDataOr(Paths.get(userHome, ".local", "share"),
                    "com.modrinth.ModrinthApp");
        };
    }

    // ----------------------------------------------------------- CurseForge

    /**
     * Типичные корни {@code .../minecraft}, внутри которых лежит {@code Instances/}
     * (Overwolf CurseForge App). Порядок — от более специфичных к общим.
     */
    public List<Path> curseForgeMinecraftBases() {
        List<Path> raw = new ArrayList<>(4);
        switch (family) {
            case WINDOWS -> {
                if (!nullOrBlank(localAppData)) {
                    raw.add(Paths.get(localAppData, "curseforge", "minecraft"));
                }
                raw.add(Paths.get(userHome, "curseforge", "minecraft"));
                raw.add(Paths.get(userHome, "Documents", "curseforge", "minecraft"));
            }
            case MACOS -> raw.add(Paths.get(userHome, "Documents", "curseforge", "minecraft"));
            case LINUX -> {
                raw.add(xdgDataOr(Paths.get(userHome, ".local", "share"), "curseforge")
                        .resolve("minecraft"));
                raw.add(Paths.get(userHome, "Documents", "curseforge", "minecraft"));
            }
        }
        Set<Path> uniq = new LinkedHashSet<>();
        for (Path p : raw) {
            uniq.add(p.toAbsolutePath().normalize());
        }
        return List.copyOf(uniq);
    }

    /** Первый типичный кандидат (совместимость с вызовами «один путь»). */
    public Path curseforgeInstances() {
        List<Path> c = curseForgeMinecraftBases();
        return c.isEmpty() ? Paths.get(userHome, "curseforge", "minecraft") : c.get(0);
    }

    // ----------------------------------------------------------- MultiMC

    /**
     * Стандартные data-dir MultiMC (не portable). Portable-установку пользователь
     * всё равно указывает вручную через {@code --dir}.
     */
    public List<Path> multiMcDataDirCandidates() {
        return switch (family) {
            case WINDOWS -> List.of(
                    Paths.get(nullOrBlank(appData) ? userHome : appData, "MultiMC"));
            case MACOS -> List.of(Paths.get(userHome, "Library", "Application Support", "MultiMC"));
            case LINUX -> List.of(
                    xdgDataOr(Paths.get(userHome, ".local", "share"), "multimc"),
                    Paths.get(userHome, ".multimc"));
        };
    }

    // ----------------------------------------------------------- helpers

    private Path xdgDataOr(Path fallback, String subdir) {
        if (!nullOrBlank(xdgData)) {
            return Paths.get(xdgData, subdir);
        }
        return fallback.resolve(subdir);
    }

    private static boolean nullOrBlank(String s) {
        return s == null || s.isBlank();
    }
}
