/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.mc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * JVM-флаги Vida для инсталлятора: диапазон поддерживаемых версий игры и проброс
 * {@code -Dvida.minecraftVersion} / {@code -Dvida.platformProfile}, согласованные с
 * {@code dev.vida.gradle.VidaPlugin#inferPlatformProfileFromGameVersion}.
 */
public final class VidaInstallerJvm {

    private static final Pattern TRIPLE = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");

    private VidaInstallerJvm() {}

    /**
     * Допустимые версии для полей инсталлятора: {@code 1.21.1+} в линии 1.21.x и
     * {@code 26.1.0}–{@code 26.1.2}, плюс превью {@code 26.1.preview}.
     */
    public static boolean isSupportedGameVersion(String raw) {
        if (raw == null) {
            return false;
        }
        String v = raw.trim();
        if (v.isEmpty()) {
            return false;
        }
        if ("26.1.preview".equals(v)) {
            return true;
        }
        var m = TRIPLE.matcher(v);
        if (!m.matches()) {
            return false;
        }
        int maj = Integer.parseInt(m.group(1));
        int min = Integer.parseInt(m.group(2));
        int pat = Integer.parseInt(m.group(3));
        if (maj == 1 && min == 21 && pat >= 1) {
            return true;
        }
        return maj == 26 && min == 1 && pat >= 0 && pat <= 2;
    }

    /**
     * Идентификатор профиля платформы для classpath ({@code META-INF/vida/platform-profiles/...}),
     * или {@code null} если профиль по версии не выводится.
     */
    public static String inferPlatformProfileId(String minecraftVersion) {
        if (!isSupportedGameVersion(minecraftVersion)) {
            return null;
        }
        String t = minecraftVersion.trim();
        if ("26.1.preview".equals(t)) {
            return "calendar-26/26.1.preview";
        }
        if (t.matches("1\\.21\\.\\d+")) {
            return "legacy-121/" + t;
        }
        if (t.matches("26\\.1\\.\\d+")) {
            return "calendar-26/" + t;
        }
        return null;
    }

    /** Дополнительные элементы для {@code arguments.jvm} Mojang {@code version.json} (без дубликатов). */
    public static void addAliasAndProfileJvmArgs(List<Object> jvm, String minecraftVersion) {
        String mv = minecraftVersion.trim();
        jvm.add("-Dvida.minecraftVersion=" + mv);
        String pf = inferPlatformProfileId(mv);
        if (pf != null) {
            jvm.add("-Dvida.platformProfile=" + pf);
        }
    }

    /**
     * Один пробел между токенами; для ATLauncher / Prism {@code JvmArgs} / CurseForge
     * {@code javaArgsOverride}.
     */
    public static String spaceSeparatedInstallerJvmProps(String minecraftVersion, String loaderVersion) {
        String mv = minecraftVersion.trim();
        String lv = loaderVersion.trim();
        StringBuilder sb = new StringBuilder();
        sb.append("-Dvida.loader.version=").append(lv);
        sb.append(" -Dvida.minecraft.version=").append(mv);
        sb.append(" -Dvida.minecraftVersion=").append(mv);
        String pf = inferPlatformProfileId(mv);
        if (pf != null) {
            sb.append(" -Dvida.platformProfile=").append(pf);
        }
        return sb.toString();
    }

    /**
     * Токены без пробелов — удобно для Modrinth {@code override_extra_launch_args} (JSON-массив).
     */
    public static List<String> installerJvmPropTokens(String minecraftVersion, String loaderVersion) {
        String s = spaceSeparatedInstallerJvmProps(minecraftVersion, loaderVersion);
        List<String> out = new ArrayList<>();
        for (String tok : s.split(" ")) {
            if (!tok.isEmpty()) {
                out.add(tok);
            }
        }
        return out;
    }

    /** Открыто для патчей лаунчеров сне JSON-массива JVM-аргументов (Modrinth App). */
    public static boolean isManagedVidaInstallerJvmToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        String t = token;
        Locale loc = Locale.ROOT;
        if (t.startsWith("-Dvida.loader.version=")) {
            return true;
        }
        if (t.startsWith("-Dvida.minecraft.version=")) {
            return true;
        }
        if (t.startsWith("-Dvida.minecraftVersion=")) {
            return true;
        }
        return t.toLowerCase(loc).startsWith("-dvida.platformprofile=");
    }

    /** Удаляет ранее прописанные инсталлятором {@code -Dvida.*} токены (split по пробелу). */
    public static String stripManagedInstallerJvmTokens(String spaceSeparatedArgs) {
        if (spaceSeparatedArgs == null || spaceSeparatedArgs.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String tok : spaceSeparatedArgs.trim().split(" +")) {
            if (tok.isBlank() || isManagedVidaInstallerJvmToken(tok)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(tok);
        }
        return sb.toString();
    }
}
