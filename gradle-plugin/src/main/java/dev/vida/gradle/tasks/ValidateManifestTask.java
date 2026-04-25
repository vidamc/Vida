/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle.tasks;

import dev.vida.manifest.ManifestError;
import dev.vida.fuente.FuenteManifestoDatapackValidador;
import dev.vida.manifest.ManifestParser;
import dev.vida.manifest.ModManifest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/**
 * Валидирует {@code vida.mod.json} через {@link ManifestParser}.
 *
 * <p>Читает файл, построенный {@link GenerateManifestTask} (или
 * написанный моддером вручную), и при ошибке кидает {@link GradleException}
 * с понятным описанием.
 */
@DisableCachingByDefault(because = "Reads manifest and optional resources for validation only.")
public abstract class ValidateManifestTask extends DefaultTask {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getManifestFile();

    /**
     * Корень resources (часто {@code src/main/resources}) для Fuente datapack-check.
     * {@code @Internal}: каталог может ещё не существовать; корректность и так
     * проверяется в теле задачи.
     */
    @Internal
    public abstract DirectoryProperty getResourcesRoot();

    public ValidateManifestTask() {
        setGroup("vida");
        setDescription("Validate vida.mod.json against the schema.");
    }

    @TaskAction
    public void validate() throws IOException {
        Path f = getManifestFile().get().getAsFile().toPath();
        if (!Files.exists(f)) {
            throw new GradleException("vida.mod.json not found at " + f);
        }
        String json = Files.readString(f, StandardCharsets.UTF_8);
        var res = ManifestParser.parse(json);
        if (res.isErr()) {
            ManifestError err = res.unwrapErr();
            throw new GradleException("vida.mod.json at " + f + " is invalid:\n  " + err);
        }
        ModManifest m = res.unwrap();
        getLogger().lifecycle("vida.mod.json ok — id='{}' version='{}' name='{}'",
                m.id(), m.version(), m.name());

        Path resources = getResourcesRoot().getOrNull() == null
                ? null
                : getResourcesRoot().get().getAsFile().toPath();
        if (resources != null && Files.isDirectory(resources)) {
            FuenteManifestoDatapackValidador.validarRecursos(m, resources).ifPresent(msg -> {
                throw new GradleException("vida:dataDriven validation failed: " + msg);
            });
        }
    }
}
