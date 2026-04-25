/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.mc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Построитель стандартного Minecraft version-JSON для Vida.
 *
 * <p>Совместим с Mojang Launcher (schema используется с 1.13+), MultiMC,
 * Prism и ATLauncher. Ключевые особенности получившегося файла:
 * <ul>
 *   <li>{@code inheritsFrom: "1.21.1"} — ванильный профиль остаётся
 *       источником JVM-классов, манёвров авторизации и ассетов;</li>
 *   <li>{@code libraries} содержит одну запись — наш loader в Maven-координатах
 *       {@code dev.vida:vida-loader:<ver>}, с {@code sha1} и {@code size};</li>
 *   <li>{@code arguments.jvm} добавляет {@code -javaagent:...} через
 *       placeholder {@code ${library_directory}}, который лаунчеры раскрывают
 *       в абсолютный путь, плюс полезные {@code -D}-флаги Vida;</li>
 *   <li>{@code mainClass} наследуется от 1.21.1 (не переопределяем).</li>
 * </ul>
 */
public final class VersionJson {

    private VersionJson() {}

    /** Неизменяемые входные данные для писателя. */
    public record Params(
            McLayout layout,
            String sha1Hex,
            long sizeBytes,
            Instant releaseTime) {
        public Params {
            Objects.requireNonNull(layout, "layout");
            Objects.requireNonNull(sha1Hex, "sha1Hex");
            Objects.requireNonNull(releaseTime, "releaseTime");
            if (sizeBytes < 0) throw new IllegalArgumentException("sizeBytes < 0");
        }
    }

    /** Сборка версии в виде tree-model (готовой к {@link JsonTree#write(Object)}). */
    public static Map<String, Object> build(Params p) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", p.layout.profileId());
        root.put("inheritsFrom", p.layout.minecraftVersion());
        root.put("type", "release");
        String ts = p.releaseTime.toString();
        root.put("time", ts);
        root.put("releaseTime", ts);

        // arguments.jvm — массив строк. Ванильный launcher сам подставит
        // ${library_directory}, ${game_directory}, ${classpath} и другие
        // placeholder'ы при запуске.
        Map<String, Object> arguments = new LinkedHashMap<>();
        List<Object> jvm = new ArrayList<>();
        jvm.add("-javaagent:${library_directory}/" + p.layout.libraryRelativePath());
        jvm.add("-Dvida.mods=${game_directory}/mods");
        jvm.add("-Dvida.config=${game_directory}/vida/config");
        jvm.add("-Dvida.loader.version=" + p.layout.loaderVersion());
        jvm.add("-Dvida.minecraft.version=" + p.layout.minecraftVersion());
        VidaInstallerJvm.addAliasAndProfileJvmArgs(jvm, p.layout.minecraftVersion());
        arguments.put("jvm", jvm);
        root.put("arguments", arguments);

        // libraries — одна запись, наш loader.
        Map<String, Object> artifact = new LinkedHashMap<>();
        artifact.put("path", p.layout.libraryRelativePath());
        artifact.put("sha1", p.sha1Hex);
        artifact.put("size", p.sizeBytes);
        // url пустой: launcher НЕ должен его скачивать — файл положен локально.
        artifact.put("url", "");
        Map<String, Object> downloads = new LinkedHashMap<>();
        downloads.put("artifact", artifact);
        Map<String, Object> library = new LinkedHashMap<>();
        library.put("name", p.layout.loaderMavenCoord());
        library.put("downloads", downloads);
        List<Object> libs = new ArrayList<>();
        libs.add(library);
        root.put("libraries", libs);

        return root;
    }

    /** Удобный one-shot JSON-рендеринг. */
    public static String render(Params p) {
        return JsonTree.write(build(p));
    }
}
