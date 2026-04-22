/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle.internal;

import dev.vida.cartografia.MappingTree;
import dev.vida.cartografia.Namespace;
import dev.vida.cartografia.asm.CartografiaRemapper;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;

/**
 * Применяет {@link CartografiaRemapper} к каждому {@code .class}-entry
 * в jar/zip и переписывает остальные записи без изменений.
 *
 * <p>Гарантии:
 * <ul>
 *   <li>порядок записей сохраняется → детерминированный output;</li>
 *   <li>{@code META-INF/MANIFEST.MF}, сигнатуры и ресурсы не трогаются;</li>
 *   <li>ошибки чтения классов логируются и считаются, но не прерывают
 *       весь процесс — плохой класс копируется as-is (с отметкой в счётчике
 *       {@link Report#errores}).</li>
 * </ul>
 */
public final class JarRemapEngine {

    private JarRemapEngine() {}

    /** Итоги ремаппинга одного jar'а. */
    public record Report(int entradas, int clases, int remapeadas, int errores) {}

    /**
     * Применяет {@code remapper} к jar-файлу.
     *
     * @param input  исходный jar
     * @param output целевой jar (будет перезаписан)
     * @param tree   дерево мэппингов
     * @param target имя namespace, куда переводим
     */
    public static Report remap(Path input, Path output, MappingTree tree, Namespace target)
            throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(tree, "tree");
        Objects.requireNonNull(target, "target");

        CartografiaRemapper remapper = CartografiaRemapper.of(tree, target);

        AtomicInteger entradas = new AtomicInteger();
        AtomicInteger clases   = new AtomicInteger();
        AtomicInteger remap    = new AtomicInteger();
        AtomicInteger errores  = new AtomicInteger();

        Files.createDirectories(output.toAbsolutePath().getParent());

        try (ZipFile zin = new ZipFile(input.toFile());
             ZipOutputStream zout = new ZipOutputStream(
                     new BufferedOutputStream(Files.newOutputStream(output)))) {

            Enumeration<? extends ZipEntry> e = zin.entries();
            while (e.hasMoreElements()) {
                ZipEntry ze = e.nextElement();
                entradas.incrementAndGet();
                if (ze.isDirectory()) {
                    zout.putNextEntry(new ZipEntry(ze.getName()));
                    zout.closeEntry();
                    continue;
                }
                try (InputStream in = new BufferedInputStream(zin.getInputStream(ze))) {
                    byte[] raw = in.readAllBytes();
                    if (ze.getName().endsWith(".class")) {
                        clases.incrementAndGet();
                        byte[] translated = translateClass(raw, remapper);
                        if (translated != null) {
                            String mappedName = remapper.map(stripClassSuffix(ze.getName()));
                            ZipEntry out = new ZipEntry(mappedName + ".class");
                            zout.putNextEntry(out);
                            zout.write(translated);
                            zout.closeEntry();
                            remap.incrementAndGet();
                            continue;
                        } else {
                            errores.incrementAndGet();
                            // fall through → copy as-is
                        }
                    }
                    ZipEntry out = new ZipEntry(ze.getName());
                    zout.putNextEntry(out);
                    zout.write(raw);
                    zout.closeEntry();
                } catch (IOException io) {
                    errores.incrementAndGet();
                    throw io;
                }
            }
        }
        return new Report(entradas.get(), clases.get(), remap.get(), errores.get());
    }

    // ============================================================ internal

    private static byte[] translateClass(byte[] bytes, CartografiaRemapper remapper) {
        try {
            ClassReader reader = new ClassReader(bytes);
            // COMPUTE_MAXS — достаточно: сами фреймы не меняются от переименования.
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            ClassRemapper rem = new ClassRemapper(writer, remapper);
            reader.accept(rem, 0);
            return writer.toByteArray();
        } catch (RuntimeException ignore) {
            // Невалидный/защищённый класс — вернём null, пусть движок копирует as-is.
            return null;
        }
    }

    private static String stripClassSuffix(String name) {
        return name.endsWith(".class")
                ? name.substring(0, name.length() - ".class".length())
                : name;
    }
}
