/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Точка входа Vida как Java-агента. Манифест модуля {@code :loader}
 * прописывает этот класс в {@code Premain-Class} и {@code Agent-Class}.
 *
 * <p>Приём опций — через {@code agentArgs} (строка, передаваемая после
 * {@code -javaagent:vida-loader.jar=...}) и/или через системные свойства:
 * <ul>
 *   <li>{@code vida.modsDir}         — путь к директории модов;</li>
 *   <li>{@code vida.cacheDir}        — путь к кешу;</li>
 *   <li>{@code vida.gameJars}        — список путей через
 *       {@link java.io.File#pathSeparatorChar};</li>
 *   <li>{@code vida.extraSources}    — аналогично для дополнительных
 *       источников модов;</li>
 *   <li>{@code vida.strict}          — {@code true/false}.</li>
 * </ul>
 *
 * <p>Формат {@code agentArgs}: пары {@code key=value}, разделённые
 * запятой, например {@code modsDir=./mods,strict=true}. Системные
 * свойства переопределяются значениями из {@code agentArgs}.
 *
 * <p>Если {@code modsDir} не задан явно и {@code skipDiscovery} не выставлен,
 * агент пытается авто-определить директорию модов: проверяет, существует ли
 * подпапка {@code mods/} относительно {@code user.dir} (рабочей директории
 * процесса). Стандартные MC-запускалки (Mojang Launcher, MultiMC, Prism) всегда
 * устанавливают {@code user.dir} в корень профиля игры, где и находится
 * {@code mods/}.
 */
@ApiStatus.Preview("loader")
public final class VidaPremain {

    private static final Log LOG = Log.of(VidaPremain.class);

    private VidaPremain() {}

    /** Вызывается JVM при старте с {@code -javaagent}. */
    public static void premain(String agentArgs, Instrumentation inst) {
        runAgent(agentArgs, inst, "premain");
    }

    /** Вызывается, когда агент аттачится к уже запущенному процессу. */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        runAgent(agentArgs, inst, "agentmain");
    }

    // ----------------------------------------------------------------

    private static void runAgent(String agentArgs, Instrumentation inst, String source) {
        LOG.info("VidaPremain.{}(agentArgs={})", source, agentArgs);
        BootOptions options = autoDetectModsDir(buildOptions(agentArgs));
        BootReport report = VidaBoot.boot(options, ClassLoader.getSystemClassLoader(), inst);
        if (!report.isOk()) {
            // В strict-режиме или при явной ошибке бросаем — JVM упадёт с
            // внятным сообщением, это лучше, чем падение где-то глубже.
            if (options.strict()) {
                throw new IllegalStateException(
                        "Vida boot failed with " + report.errors().size() + " errors");
            }
        }
    }

    static BootOptions buildOptions(String agentArgs) {
        var b = BootOptions.builder();

        // Системные свойства — базовый слой.
        String modsDir    = System.getProperty("vida.modsDir");
        String cacheDir   = System.getProperty("vida.cacheDir");
        String gameJars   = System.getProperty("vida.gameJars");
        String extras     = System.getProperty("vida.extraSources");
        String strict     = System.getProperty("vida.strict");
        String skipDisc   = System.getProperty("vida.skipDiscovery");
        String vidaVer    = System.getProperty("vida.version");
        String mcVer      = System.getProperty("vida.minecraftVersion");

        // agentArgs перекрывает системные свойства.
        if (agentArgs != null && !agentArgs.isBlank()) {
            for (String pair : agentArgs.split(",")) {
                int eq = pair.indexOf('=');
                if (eq <= 0) continue;
                String k = pair.substring(0, eq).trim();
                String v = pair.substring(eq + 1).trim();
                switch (k) {
                    case "modsDir"          -> modsDir = v;
                    case "cacheDir"         -> cacheDir = v;
                    case "gameJars"         -> gameJars = v;
                    case "extraSources"     -> extras = v;
                    case "strict"           -> strict = v;
                    case "skipDiscovery"    -> skipDisc = v;
                    case "vidaVersion"      -> vidaVer = v;
                    case "minecraftVersion" -> mcVer = v;
                    default -> LOG.warn("Vida: unknown agent arg '{}'", k);
                }
            }
        }

        if (modsDir != null && !modsDir.isBlank()) b.modsDir(Path.of(modsDir));
        if (cacheDir != null && !cacheDir.isBlank()) b.cacheDir(Path.of(cacheDir));
        for (Path p : BootOptions.parsePathList(gameJars)) b.addGameJar(p);
        for (Path p : BootOptions.parsePathList(extras))   b.addExtraSource(p);
        if (parseBool(strict))   b.strict(true);
        if (parseBool(skipDisc)) b.skipDiscovery(true);
        if (vidaVer != null) b.vidaVersion(vidaVer);
        if (mcVer   != null) b.minecraftVersion(mcVer);

        return b.build();
    }

    /**
     * Если {@code modsDir} не задан явно, пробует найти {@code ./mods} относительно
     * {@code user.dir}. Это безопасно: метод только читает файловую систему,
     * ничего не создаёт и не изменяет.
     */
    private static BootOptions autoDetectModsDir(BootOptions options) {
        if (options.skipDiscovery() || options.modsDir() != null) return options;
        try {
            Path candidate = Path.of(System.getProperty("user.dir", ".")).resolve("mods");
            if (Files.isDirectory(candidate)) {
                LOG.info("Vida: auto-detected modsDir='{}'", candidate);
                return options.withModsDir(candidate);
            }
        } catch (SecurityException ignored) {
            // Среды с SecurityManager могут запрещать чтение свойств/файловой системы.
        }
        return options;
    }

    private static boolean parseBool(String s) {
        return s != null && (s.equalsIgnoreCase("true") || s.equals("1")
                || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("on"));
    }
}
