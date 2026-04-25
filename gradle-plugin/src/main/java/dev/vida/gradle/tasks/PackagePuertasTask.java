/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Копирует все валидные {@code .ptr}-файлы в папку упаковки ресурсов.
 *
 * <p>Сохраняет иерархию путей как есть: {@code puertas/api.ptr} в
 * {@code src/main/resources} попадёт в {@code <output>/puertas/api.ptr}.
 * Это упрощает {@code processResources}: он подхватит всю папку через
 * {@code srcDir}.
 *
 * <p>Задача предполагает, что перед ней уже отработала
 * {@link ValidatePuertasTask} — если файл невалиден, сборка упадёт раньше.
 */
@DisableCachingByDefault(because = "Copies selected .ptr files into the resource output tree.")
public abstract class PackagePuertasTask extends DefaultTask {

    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getResourcesRoot();

    @Input
    public abstract ListProperty<String> getPuertasPaths();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    public PackagePuertasTask() {
        setGroup("vida");
        setDescription("Package validated .ptr files for inclusion in the mod JAR.");
    }

    @TaskAction
    public void ejecutar() throws IOException {
        List<String> paths = getPuertasPaths().getOrElse(List.of());
        Path root = getResourcesRoot().isPresent()
                ? getResourcesRoot().get().getAsFile().toPath()
                : getProject().file("src/main/resources").toPath();
        Path out = getOutputDir().get().getAsFile().toPath();
        if (Files.exists(out)) {
            try (var walker = Files.walk(out)) {
                walker.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(p -> {
                            try { if (!p.equals(out)) Files.deleteIfExists(p); }
                            catch (IOException e) { throw new RuntimeException(e); }
                        });
            }
        }
        Files.createDirectories(out);

        int copiados = 0;
        for (String relativa : paths) {
            Path origen = root.resolve(relativa).toAbsolutePath();
            if (!Files.isRegularFile(origen)) {
                throw new GradleException(".ptr не найден: " + relativa + " (искали " + origen + ")");
            }
            Path destino = out.resolve(relativa);
            Files.createDirectories(destino.getParent());
            Files.copy(origen, destino,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
            copiados++;
        }
        getLogger().lifecycle("vidaPackagePuertas: {} файл(ов) в {}", copiados, out);
    }
}
