/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle.tasks;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.DisableCachingByDefault;

/**
 * Запускает Minecraft с загрузчиком Vida.
 *
 * <p>Два режима:
 * <ul>
 *   <li>{@code agent = true} — jvm-аргумент {@code -javaagent:loader.jar}
 *       + {@link #getMainClass() mainClass} игры;</li>
 *   <li>{@code agent = false} — main-класс заменяется на
 *       {@code dev.vida.loader.VidaBoot}, которому передаются mods/game-jar
 *       через system-properties.</li>
 * </ul>
 *
 * <p>Требует явно указанных {@link #getClientJar()} и
 * {@link #getLoaderJar()} — задача не занимается скачиванием артефактов,
 * это зона ответственности моддера.
 */
@DisableCachingByDefault(because = "Runs an external JVM process; outputs are not Gradle build outputs.")
public abstract class RunVidaTask extends JavaExec {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getClientJar();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getLoaderJar();

    @Classpath
    public abstract ConfigurableFileCollection getModJars();

    @InputFile @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getModsDir();

    @Input
    public abstract Property<Boolean> getAgent();

    @Input
    public abstract Property<Boolean> getStrict();

    @Input
    public abstract ListProperty<String> getVidaJvmArgs();

    @Input
    public abstract ListProperty<String> getVidaArgs();

    @Input
    public abstract Property<String> getBootMainClass();

    @Input
    public abstract Property<String> getVidaMainClass();

    /** Пробрасывается в JVM как {@code -Dvida.accessDenied=…}. */
    @Input
    public abstract ListProperty<String> getAccessDeniedIds();

    /** Пробрасывается в JVM как {@code -Dvida.minecraftVersion=…}. */
    @Input
    @Optional
    public abstract Property<String> getMinecraftVersion();

    /** Пробрасывается в JVM как {@code -Dvida.platformProfile=…}. */
    @Input
    @Optional
    public abstract Property<String> getPlatformProfile();

    /** Включает {@code -Dvida.dev.hotReload=true}. */
    @Input
    public abstract Property<Boolean> getHotReload();

    /**
     * Каталог скомпилированных классов для наблюдения ({@code build/classes/java/main}).
     */
    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getHotReloadWatchDir();

    public RunVidaTask() {
        setGroup("vida");
        setDescription("Run Minecraft with Vida loader attached.");
        // Дефолты выставит plugin. Блокируем авто-закрытие стандартных ChildProcess.
        getBootMainClass().convention("dev.vida.loader.VidaBoot");
        getAccessDeniedIds().convention(List.of());
        getHotReload().convention(false);
    }

    @Override
    public void exec() {
        File client = getClientJar().get().getAsFile();
        File loader = getLoaderJar().get().getAsFile();

        if (!client.exists()) {
            throw new GradleException("Minecraft client jar not found: " + client
                    + "\n  Set 'vida.minecraft.clientJar' to the correct path.");
        }
        if (!loader.exists()) {
            throw new GradleException("Vida loader jar not found: " + loader
                    + "\n  This should come from the :loader module. Build it first.");
        }

        DirectoryProperty wd = getWorkingDir() == null ? null : null;
        File workDir = getWorkingDir();
        if (workDir != null) {
            try { Files.createDirectories(workDir.toPath()); }
            catch (Exception e) { throw new GradleException("Cannot create workingDir " + workDir, e); }
        }

        boolean agent  = getAgent().get();
        boolean strict = getStrict().get();

        // classpath всегда содержит клиент + моды + (в non-agent-режиме) loader.
        List<File> cp = new ArrayList<>();
        cp.add(client);
        getModJars().forEach(cp::add);
        if (!agent) cp.add(loader);
        classpath(cp.toArray());

        // JVM-аргументы
        List<String> jvm = new ArrayList<>(getVidaJvmArgs().get());
        List<String> denied = getAccessDeniedIds().get();
        if (denied != null && !denied.isEmpty()) {
            jvm.add("-Dvida.accessDenied=" + String.join(",", denied));
        }
        appendOptionalSysProp(jvm, getMinecraftVersion(), "vida.minecraftVersion");
        appendOptionalSysProp(jvm, getPlatformProfile(), "vida.platformProfile");
        if (Boolean.TRUE.equals(getHotReload().getOrElse(false))) {
            jvm.add("-Dvida.dev.hotReload=true");
            if (getHotReloadWatchDir().isPresent()) {
                jvm.add("-Dvida.dev.hotReload.watch="
                        + getHotReloadWatchDir().get().getAsFile().getAbsolutePath());
            }
        }
        if (agent) {
            StringBuilder agentArg = new StringBuilder("-javaagent:");
            agentArg.append(loader.getAbsolutePath());
            if (getModsDir().isPresent()) {
                agentArg.append("=modsDir=").append(getModsDir().get().getAsFile().getAbsolutePath());
                agentArg.append(",strict=").append(strict);
            } else {
                agentArg.append("=strict=").append(strict);
            }
            jvm.add(agentArg.toString());
        } else {
            if (getModsDir().isPresent()) {
                jvm.add("-Dvida.mods=" + getModsDir().get().getAsFile().getAbsolutePath());
            }
            jvm.add("-Dvida.strict=" + strict);
            jvm.add("-Dvida.game=" + client.getAbsolutePath());
        }
        setJvmArgs(jvm);

        // Main-класс
        if (agent) {
            getMainClass().set(getVidaMainClass());
        } else {
            getMainClass().set(getBootMainClass());
        }

        setArgs(new ArrayList<>(getVidaArgs().get()));

        getLogger().lifecycle("Launching Vida: agent={} strict={} mainClass={} cpSize={}",
                agent, strict, getMainClass().get(), cp.size());

        super.exec();
    }

    private static void appendOptionalSysProp(List<String> jvm, Property<String> prop, String key) {
        if (!prop.isPresent()) {
            return;
        }
        String v = prop.get();
        if (v == null || v.isBlank()) {
            return;
        }
        jvm.add("-D" + key + "=" + v.trim());
    }
}
