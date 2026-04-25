/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle.tasks;

import dev.vida.gradle.internal.ManifestJsonWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/**
 * Генерирует {@code vida.mod.json} из DSL-блока {@code vida.mod}.
 *
 * <p>Задача регистрируется автоматически; явно вызывать её из build-скриптов
 * не требуется — {@code processResources} её {@code dependsOn}'ит.
 */
@DisableCachingByDefault(because = "Generates vida.mod.json from DSL inputs.")
public abstract class GenerateManifestTask extends DefaultTask {

    @Input public abstract Property<Integer> getSchema();
    @Input public abstract Property<String> getModId();
    @Input public abstract Property<String> getModVersion();
    @Input public abstract Property<String> getDisplayName();
    @Input @Optional public abstract Property<String> getModDescription();
    @Input @Optional public abstract Property<String> getLicense();
    @Input @Optional public abstract Property<String> getEntrypoint();
    @Input public abstract ListProperty<String> getEntrypointsPreLaunch();
    @Input public abstract ListProperty<String> getEntrypointsMain();
    @Input public abstract ListProperty<String> getEntrypointsClient();
    @Input public abstract ListProperty<String> getEntrypointsServer();
    @Input public abstract ListProperty<String> getAuthors();
    @Input public abstract ListProperty<String> getPuertas();
    @Input public abstract ListProperty<String> getEscultores();
    @Input public abstract ListProperty<String> getVifadaPackages();
    @Input @Optional public abstract Property<String> getVifadaConfig();
    @Input public abstract Property<Integer> getVifadaPriority();
    @Input public abstract ListProperty<String> getModules();
    @Input public abstract MapProperty<String, String> getDependencies();
    @Input public abstract MapProperty<String, String> getOptionalDependencies();
    @Input public abstract ListProperty<String> getIncompatibilities();

    @Input public abstract Property<Boolean> getInjectDefaultVidaDependency();

    @Input public abstract Property<String> getDefaultVidaDependencyRange();

    @OutputFile public abstract RegularFileProperty getOutput();

    public GenerateManifestTask() {
        setGroup("vida");
        setDescription("Generate vida.mod.json from the 'vida { mod { ... } }' DSL.");
    }

    @TaskAction
    public void generate() throws IOException {
        LinkedHashMap<String, String> deps =
                new LinkedHashMap<>(Map.copyOf(getDependencies().get()));
        if (Boolean.TRUE.equals(getInjectDefaultVidaDependency().getOrElse(true))) {
            deps.putIfAbsent("vida", getDefaultVidaDependencyRange().getOrElse("^0.1.0"));
        }

        String json = ManifestJsonWriter.toJson(ManifestJsonWriter.draftFromModSpec(
                getSchema().get(),
                getModId().get(),
                getModVersion().get(),
                getDisplayName().get(),
                getModDescription().getOrElse(""),
                getLicense().getOrElse(""),
                getEntrypoint().getOrElse(""),
                List.copyOf(getEntrypointsPreLaunch().get()),
                List.copyOf(getEntrypointsMain().get()),
                List.copyOf(getEntrypointsClient().get()),
                List.copyOf(getEntrypointsServer().get()),
                List.copyOf(getAuthors().get()),
                List.copyOf(getPuertas().get()),
                List.copyOf(getEscultores().get()),
                List.copyOf(getVifadaPackages().get()),
                getVifadaConfig().getOrElse(""),
                getVifadaPriority().get(),
                List.copyOf(getModules().get()),
                Map.copyOf(deps),
                Map.copyOf(getOptionalDependencies().get()),
                List.copyOf(getIncompatibilities().get())));

        Path out = getOutput().get().getAsFile().toPath();
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, json, StandardCharsets.UTF_8);
        getLogger().lifecycle("Wrote {} ({} bytes)", out, json.length());
    }
}
