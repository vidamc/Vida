/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers;

import java.util.Locale;

/**
 * Перечень поддерживаемых лаунчеров Minecraft.
 *
 * <p>Каждый лаунчер имеет собственную архитектуру:
 * <ul>
 *   <li>{@link #MOJANG} — централизованный {@code .minecraft} с общими
 *       {@code libraries/}, {@code versions/} и {@code launcher_profiles.json}.</li>
 *   <li>{@link #PRISM} / {@link #MULTIMC} — каталог {@code instances/<name>/}
 *       с {@code instance.cfg}, {@code mmc-pack.json} и опциональными
 *       {@code patches/<uid>.json}. Prism поддерживает компонент с
 *       {@code +agents}, MultiMC — только {@code JvmArgs} в {@code instance.cfg}.</li>
 *   <li>{@link #ATLAUNCHER} — один {@code instance.json} (Gson-блоб) на
 *       каждый instance; лаунчер перезаписывает его при запуске, поэтому
 *       создавать с нуля нецелесообразно — мы только <em>патчим существующий</em>.</li>
 *   <li>{@link #MODRINTH} — профили хранятся в SQLite ({@code app.db}),
 *       а не в файловой системе; JDBC (sqlite-jdbc) в fat-jar инсталлятора.</li>
 *   <li>{@link #CURSEFORGE} — {@code minecraftinstance.json} на instance;
 *       {@code javaArgsOverride} для {@code -javaagent} (см. {@code CurseForgeHandler}).</li>
 * </ul>
 */
public enum LauncherKind {

    MOJANG("minecraft", "Minecraft Launcher", true),
    PRISM("prism", "Prism Launcher", true),
    MULTIMC("multimc", "MultiMC", true),
    ATLAUNCHER("atlauncher", "ATLauncher", true),
    MODRINTH("modrinth", "Modrinth App", true),
    CURSEFORGE("curseforge", "CurseForge App", true);

    private final String cliName;
    private final String displayName;
    private final boolean implemented;

    LauncherKind(String cliName, String displayName, boolean implemented) {
        this.cliName = cliName;
        this.displayName = displayName;
        this.implemented = implemented;
    }

    /** Короткое имя для CLI-флага {@code --launcher <name>}. */
    public String cliName()     { return cliName; }

    /** Отображаемое имя в GUI. */
    public String displayName() { return displayName; }

    /**
     * {@code true} если handler для этого лаунчера реализован.
     * Возвращает {@code false} для тех, что будут добавлены в следующих фазах.
     */
    public boolean isImplemented() { return implemented; }

    /** Case-insensitive парсер для CLI. */
    public static LauncherKind fromCli(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (LauncherKind k : values()) {
            if (k.cliName.equals(lower) || k.name().toLowerCase(Locale.ROOT).equals(lower)) {
                return k;
            }
        }
        throw new IllegalArgumentException(
                "Unknown launcher: '" + name + "'. Available: "
                        + String.join(", ", cliNamesList()));
    }

    /** Список CLI-имён (для сообщений об ошибках и --help). */
    public static String[] cliNamesList() {
        LauncherKind[] v = values();
        String[] out = new String[v.length];
        for (int i = 0; i < v.length; i++) out[i] = v[i].cliName;
        return out;
    }
}
