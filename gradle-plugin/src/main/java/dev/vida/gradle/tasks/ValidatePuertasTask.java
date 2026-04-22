/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle.tasks;

import dev.vida.puertas.PuertaArchivo;
import dev.vida.puertas.PuertaError;
import dev.vida.puertas.PuertaParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

/**
 * Парсит и валидирует все {@code .ptr} (puertas) файлы проекта.
 *
 * <p>Список путей берётся из {@code vida.mod.puertas} (относительно
 * {@link #getResourcesRoot}, по-умолчанию — {@code src/main/resources}).
 *
 * <p>В случае ошибок парсинга задача падает с понятным сообщением,
 * в котором видно имя файла, номер строки и сам текст ошибки.
 */
public abstract class ValidatePuertasTask extends DefaultTask {

    /** Корень, относительно которого разрешаются пути из манифеста. */
    @InputDirectory
    @Optional
    public abstract DirectoryProperty getResourcesRoot();

    /** Пути, указанные в {@code mod { puertas = [...] }}. */
    @Input
    public abstract ListProperty<String> getPuertasPaths();

    public ValidatePuertasTask() {
        setGroup("vida");
        setDescription("Validate .ptr access-widener files against the Vida schema.");
    }

    @TaskAction
    public void validate() throws IOException {
        List<String> paths = getPuertasPaths().getOrElse(List.of());
        if (paths.isEmpty()) {
            getLogger().info("No puertas files configured — skipping.");
            return;
        }

        Path root = getResourcesRoot().isPresent()
                ? getResourcesRoot().get().getAsFile().toPath()
                : getProject().file("src/main/resources").toPath();

        List<String> fallos = new ArrayList<>();
        int totalDirectivas = 0;

        for (String relativa : paths) {
            Path p = root.resolve(relativa).toAbsolutePath();
            if (!Files.isRegularFile(p)) {
                fallos.add(" - " + relativa + " — файл не найден в " + root);
                continue;
            }

            var res = PuertaParser.parsear(p);
            if (res.esExitoso()) {
                PuertaArchivo arch = res.archivo();
                totalDirectivas += arch.directivas().size();
                getLogger().lifecycle("{} ok — {} директив, namespace={}",
                        relativa, arch.directivas().size(), arch.namespace());
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(" - ").append(relativa).append(" (").append(res.errores().size())
                        .append(" ошибок):");
                for (PuertaError e : res.errores()) {
                    sb.append("\n      ").append(e);
                }
                fallos.add(sb.toString());
            }
        }

        if (!fallos.isEmpty()) {
            throw new GradleException("puertas: валидация провалена\n"
                    + String.join("\n", fallos));
        }
        getLogger().lifecycle("puertas ok — {} файлов, {} директив всего",
                paths.size(), totalDirectivas);
    }
}
