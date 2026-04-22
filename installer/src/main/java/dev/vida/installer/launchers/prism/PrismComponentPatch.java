/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.prism;

import dev.vida.installer.mc.JsonTree;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Сборщик {@code patches/dev.vida.loader.json} — component-patch'а Vida
 * внутри Prism/MultiMC instance'а.
 *
 * <p>Формат — MultiMC JSON-Patch spec (см. {@code MultiMC/Launcher/wiki/JSON-Patches}).
 *
 * <h2>Два режима записи</h2>
 * <dl>
 *   <dt>{@code withAgents=true} (Prism ≥ 7.0)</dt>
 *   <dd>добавляет массив {@code +agents} с одним элементом, указывающим на
 *       {@code libraries/loader-&lt;ver&gt;.jar} (через {@code MMC-hint: "local"}).
 *       Launcher сам сформирует корректный {@code -javaagent:&lt;абсолютный путь&gt;}
 *       на запуске. Рекомендованный путь.</dd>
 *   <dt>{@code withAgents=false} (MultiMC / старый Prism)</dt>
 *   <dd>{@code +agents} не пишем — такие лаунчеры его не поддерживают.
 *       Компонент остаётся полезным как метаданные для вкладки «Version»;
 *       реальная инъекция {@code -javaagent} происходит через
 *       {@code JvmArgs} в {@code instance.cfg}.</dd>
 * </dl>
 *
 * <p>В обоих случаях прописываем {@code requires: net.minecraft &gt;= &lt;mcVer&gt;} и
 * {@code compatibleJavaMajors: [21]}, чтобы Prism/MultiMC заранее
 * отказались запускать на неподходящей JDK вместо загадочного
 * {@code UnsupportedClassVersionError}.
 */
public final class PrismComponentPatch {

    private final String minecraftVersion;
    private final String loaderVersion;
    private final String libraryMavenCoord;
    private final boolean withAgents;

    /**
     * @param minecraftVersion    требуемая версия MC (уйдёт в {@code requires})
     * @param loaderVersion       версия Vida loader'а
     * @param libraryMavenCoord   GAV-строка для {@code libraries.name} и
     *                            {@code agents.name} (например
     *                            {@code dev.vida:loader:0.1.0})
     * @param withAgents          {@code true} для Prism-patch с {@code +agents}
     */
    public PrismComponentPatch(String minecraftVersion,
                               String loaderVersion,
                               String libraryMavenCoord,
                               boolean withAgents) {
        this.minecraftVersion = Objects.requireNonNull(minecraftVersion);
        this.loaderVersion = Objects.requireNonNull(loaderVersion);
        this.libraryMavenCoord = Objects.requireNonNull(libraryMavenCoord);
        this.withAgents = withAgents;
    }

    public String render(Instant releaseTime) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("formatVersion", 1L);
        root.put("uid", PrismInstanceLayout.COMPONENT_UID);
        root.put("name", PrismInstanceLayout.COMPONENT_NAME);
        root.put("version", loaderVersion);
        root.put("releaseTime", releaseTime.toString());
        root.put("type", "release");
        // order=20 → после net.minecraft (order=-2) и net.fabricmc.fabric-loader (order=10)
        root.put("order", 20L);

        List<Object> requires = new ArrayList<>();
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("equals", minecraftVersion);
        req.put("uid", "net.minecraft");
        requires.add(req);
        root.put("requires", requires);

        // Java 21 — жёсткое требование Vida, зашиваем в компонент.
        List<Object> javaMajors = new ArrayList<>();
        javaMajors.add(21L);
        root.put("compatibleJavaMajors", javaMajors);

        if (withAgents) {
            List<Object> agents = new ArrayList<>();
            Map<String, Object> agent = new LinkedHashMap<>();
            agent.put("name", libraryMavenCoord);
            agent.put("MMC-hint", "local");
            agent.put("argument", "");
            agents.add(agent);
            root.put("+agents", agents);
        } else {
            // Для MultiMC описываем как обычную библиотеку — хоть и не в classpath,
            // но launcher хотя бы покажет её на вкладке Versions.
            List<Object> libs = new ArrayList<>();
            Map<String, Object> lib = new LinkedHashMap<>();
            lib.put("name", libraryMavenCoord);
            lib.put("MMC-hint", "local");
            libs.add(lib);
            root.put("+mavenFiles", libs);
        }

        return JsonTree.write(root);
    }
}
