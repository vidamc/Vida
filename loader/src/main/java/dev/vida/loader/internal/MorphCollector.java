/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.internal;

import dev.vida.core.ApiStatus;
import dev.vida.discovery.ZipReader;
import dev.vida.loader.MorphIndex;
import dev.vida.vifada.MorphSource;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Сканирует содержимое JAR-архива и находит классы, помеченные
 * {@code @VifadaMorph(target = "...")}, не поднимая полноценный
 * {@code ClassNode}.
 *
 * <p>Реализация использует лёгкий {@link ClassVisitor}, который смотрит
 * только аннотации уровня класса и прерывается сразу после чтения атрибута
 * {@code target} — читать остальную часть байткода не требуется.
 */
@ApiStatus.Internal
public final class MorphCollector {

    private static final String VIFADA_MORPH_DESC = "Ldev/vida/vifada/VifadaMorph;";

    private MorphCollector() {}

    /**
     * Добавляет в {@code sink} все морфы, обнаруженные в {@code zip}.
     *
     * @param zip          открытый ридер архива
     * @param sink         куда складывать результаты (мутируется)
     * @return количество добавленных морфов
     * @throws IOException при I/O-ошибке
     */
    public static int collect(ZipReader zip, MorphIndex.Builder sink) throws IOException {
        Objects.requireNonNull(zip, "zip");
        Objects.requireNonNull(sink, "sink");
        int found = 0;
        for (String entry : zip.entries()) {
            if (!entry.endsWith(".class")) continue;
            if (entry.equals("module-info.class")) continue;
            if (entry.endsWith("package-info.class")) continue;

            byte[] bytes = zip.read(entry);
            String target = readMorphTarget(bytes);
            if (target == null) continue;

            String internalName = entry.substring(0, entry.length() - ".class".length());
            sink.add(target.replace('.', '/'),
                    new MorphSource(internalName, bytes));
            found++;
        }
        return found;
    }

    /** Собирает морфы сразу из нескольких архивов. */
    public static int collectAll(Iterable<? extends ZipReader> zips, MorphIndex.Builder sink)
            throws IOException {
        int total = 0;
        for (ZipReader z : zips) total += collect(z, sink);
        return total;
    }

    /** Возвращает {@code target} из аннотации {@code @VifadaMorph} или {@code null}, если её нет. */
    public static String readMorphTarget(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "classBytes");
        try {
            String[] holder = new String[1];
            ClassReader cr = new ClassReader(classBytes);
            cr.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (!VIFADA_MORPH_DESC.equals(desc)) return null;
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public void visit(String name, Object value) {
                            if ("target".equals(name) && value instanceof String s) {
                                holder[0] = s;
                            }
                        }
                    };
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return holder[0];
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** Публичный хелпер для unit-тестов: быстрый предикат «это морф?». */
    public static boolean isMorph(byte[] classBytes) {
        return readMorphTarget(classBytes) != null;
    }

    /** Дампит все морфы из архива в list (удобно для ручного контроля). */
    public static List<MorphSource> listMorphs(ZipReader zip) throws IOException {
        MorphIndex.Builder tmp = MorphIndex.builder();
        collect(zip, tmp);
        MorphIndex idx = tmp.build();
        java.util.ArrayList<MorphSource> out = new java.util.ArrayList<>(idx.totalMorphs());
        for (String t : idx.targets()) out.addAll(idx.forTarget(t));
        return out;
    }
}
