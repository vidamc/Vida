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
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.Optional;

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
public abstract class RunVidaTask extends JavaExec {

    @InputFile
    public abstract RegularFileProperty getClientJar();

    @InputFile
    public abstract RegularFileProperty getLoaderJar();

    @Classpath
    public abstract ConfigurableFileCollection getModJars();

    @InputFile @Optional
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

    public RunVidaTask() {
        setGroup("vida");
        setDescription("Run Minecraft with Vida loader attached.");
        // Дефолты выставит plugin. Блокируем авто-закрытие стандартных ChildProcess.
        getBootMainClass().convention("dev.vida.loader.VidaBoot");
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
        if (agent) {
            StringBuilder agentArg = new StringBuilder("-javaagent:");
            agentArg.append(loader.getAbsolutePath());
            if (getModsDir().isPresent()) {
                agentArg.append("=mods=").append(getModsDir().get().getAsFile().getAbsolutePath());
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
}
