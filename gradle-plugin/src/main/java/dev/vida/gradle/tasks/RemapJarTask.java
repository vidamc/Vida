/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle.tasks;

import dev.vida.cartografia.MappingError;
import dev.vida.cartografia.MappingTree;
import dev.vida.cartografia.Namespace;
import dev.vida.cartografia.io.CtgReader;
import dev.vida.cartografia.io.ProguardReader;
import dev.vida.core.Result;
import dev.vida.gradle.internal.JarRemapEngine;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

/**
 * Применяет маппинги к jar'у через {@link JarRemapEngine}.
 *
 * <p>Источник маппинга выбирается автоматически:
 * <ol>
 *   <li>если задан {@link #getCtgMappings()} — читается как {@code .ctg};</li>
 *   <li>иначе если задан {@link #getProguardMappings()} — читается как proguard.</li>
 * </ol>
 *
 * <p>Направление ремаппинга — из source namespace дерева в
 * {@link #getTargetNamespace() targetNamespace}. Для proguard-файлов source
 * = {@link #getObfNamespace obfNamespace} и target = {@link #getNamedNamespace
 * namedNamespace} по умолчанию.
 */
public abstract class RemapJarTask extends DefaultTask {

    @InputFile
    public abstract RegularFileProperty getInputJar();

    @OutputFile
    public abstract RegularFileProperty getOutputJar();

    @InputFile @Optional
    public abstract RegularFileProperty getProguardMappings();

    @InputFile @Optional
    public abstract RegularFileProperty getCtgMappings();

    @org.gradle.api.tasks.Input
    public abstract Property<String> getObfNamespace();

    @org.gradle.api.tasks.Input
    public abstract Property<String> getNamedNamespace();

    @org.gradle.api.tasks.Input @Optional
    public abstract Property<String> getTargetNamespace();

    public RemapJarTask() {
        setGroup("vida");
        setDescription("Apply Cartografía mappings to a .jar file.");
    }

    @TaskAction
    public void run() throws IOException {
        Path input  = getInputJar().get().getAsFile().toPath().toAbsolutePath().normalize();
        Path output = getOutputJar().get().getAsFile().toPath().toAbsolutePath().normalize();

        MappingTree tree = loadMappings();
        Namespace target = Namespace.of(
                getTargetNamespace().isPresent() ? getTargetNamespace().get() : getNamedNamespace().get());

        getLogger().lifecycle("Remapping {} -> {} (target={})",
                input.getFileName(), output.getFileName(), target);

        JarRemapEngine.Report rep = JarRemapEngine.remap(input, output, tree, target);
        getLogger().lifecycle("Remap done: {} entries, {} classes, {} remapped, {} errors",
                rep.entradas(), rep.clases(), rep.remapeadas(), rep.errores());
    }

    // ============================================================ internals

    private MappingTree loadMappings() throws IOException {
        boolean hasCtg = getCtgMappings().isPresent() && getCtgMappings().get().getAsFile().exists();
        boolean hasPg  = getProguardMappings().isPresent() && getProguardMappings().get().getAsFile().exists();
        if (!hasCtg && !hasPg) {
            throw new GradleException(
                    "No mappings configured. Set either 'vida.minecraft.mappings.ctg' or "
                    + "'vida.minecraft.mappings.proguard'.");
        }
        if (hasCtg) {
            Path f = getCtgMappings().get().getAsFile().toPath().toAbsolutePath().normalize();
            try (InputStream in = new BufferedInputStream(Files.newInputStream(f))) {
                Result<MappingTree, MappingError> r = CtgReader.read(f.getFileName().toString(), in);
                return unwrap(r, f);
            }
        }
        Path f = getProguardMappings().get().getAsFile().toPath().toAbsolutePath().normalize();
        Namespace obf   = Namespace.of(getObfNamespace().getOrElse("obf"));
        Namespace named = Namespace.of(getNamedNamespace().getOrElse("named"));
        try (var rd = new BufferedReader(
                Files.newBufferedReader(f, StandardCharsets.UTF_8))) {
            Result<MappingTree, MappingError> r =
                    ProguardReader.read(f.getFileName().toString(), rd, obf, named);
            return unwrap(r, f);
        }
    }

    private static MappingTree unwrap(Result<MappingTree, MappingError> r, Path f) {
        if (r.isErr()) {
            throw new GradleException("Failed to read mappings from " + f + ": " + r.unwrapErr());
        }
        return r.unwrap();
    }
}
