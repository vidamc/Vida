/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle.internal;

import static org.assertj.core.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

import dev.vida.cartografia.MappingTree;
import dev.vida.cartografia.Namespace;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class JarRemapEngineTest {

    @Test
    void remaps_classes_copies_resources_and_renames_entries(@TempDir Path dir) throws IOException {
        byte[] classA = buildClassA();  // com/ejemplo/A with field fooObf and method barObf()
        Path input = dir.resolve("in.jar");
        writeJar(input, Map.of(
                "com/ejemplo/A.class", classA,
                "assets/demo.txt", "hola\n".getBytes()
        ));

        MappingTree tree = buildTreeForA();
        Namespace named = Namespace.of("named");

        Path output = dir.resolve("out.jar");
        JarRemapEngine.Report rep = JarRemapEngine.remap(input, output, tree, named);

        assertThat(rep.entradas()).isEqualTo(2);
        assertThat(rep.clases()).isEqualTo(1);
        assertThat(rep.remapeadas()).isEqualTo(1);
        assertThat(rep.errores()).isZero();

        Map<String, byte[]> entries = readAll(output);
        // Класс был переименован в пути:
        assertThat(entries).doesNotContainKey("com/ejemplo/A.class");
        assertThat(entries).containsKey("com/ejemplo/Alfa.class");
        // Ресурс скопирован как есть:
        assertThat(new String(entries.get("assets/demo.txt"))).isEqualTo("hola\n");

        // Проверяем, что в байткоде поле и метод переименованы.
        byte[] renamed = entries.get("com/ejemplo/Alfa.class");
        String disasm = disasmNames(renamed);
        assertThat(disasm).contains("fooNamed").contains("barNamed");
        assertThat(disasm).doesNotContain("fooObf").doesNotContain("barObf");
    }

    @Test
    void missing_class_stays_untouched(@TempDir Path dir) throws IOException {
        // Класс, которого нет в дереве, должен остаться с прежним именем и содержимым.
        byte[] unmapped = buildSimple("com/otro/Plain");
        Path input = dir.resolve("in.jar");
        writeJar(input, Map.of("com/otro/Plain.class", unmapped));

        Path output = dir.resolve("out.jar");
        JarRemapEngine.remap(input, output, buildTreeForA(), Namespace.of("named"));

        Map<String, byte[]> entries = readAll(output);
        // Имя класса и путь остаются прежними.
        assertThat(entries).containsKey("com/otro/Plain.class");
        // Содержимое класса читается как валидный class file с тем же internal name
        // (байт-в-байт идентичность не требуется — ASM мог пересчитать константный пул).
        String cn = new ClassReader(entries.get("com/otro/Plain.class")).getClassName();
        assertThat(cn).isEqualTo("com/otro/Plain");
    }

    @Test
    void directory_entries_are_preserved(@TempDir Path dir) throws IOException {
        byte[] classA = buildClassA();
        Path input = dir.resolve("in.jar");

        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(input))) {
            z.putNextEntry(new ZipEntry("com/"));
            z.closeEntry();
            z.putNextEntry(new ZipEntry("com/ejemplo/"));
            z.closeEntry();
            z.putNextEntry(new ZipEntry("com/ejemplo/A.class"));
            z.write(classA);
            z.closeEntry();
        }

        Path output = dir.resolve("out.jar");
        JarRemapEngine.Report rep = JarRemapEngine.remap(input, output, buildTreeForA(), Namespace.of("named"));
        assertThat(rep.entradas()).isEqualTo(3);

        Map<String, byte[]> all = readAll(output);
        assertThat(all).containsKeys("com/", "com/ejemplo/");
    }

    // ================================================================ helpers

    private static MappingTree buildTreeForA() {
        Namespace obf = Namespace.of("obf");
        Namespace named = Namespace.of("named");
        return MappingTree.builder(obf, named)
                .addClass("com/ejemplo/A", "com/ejemplo/Alfa")
                    .addField("I",    "fooObf", "fooNamed")
                    .addMethod("()V", "barObf", "barNamed")
                    .done()
                .build();
    }

    private static byte[] buildClassA() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V17, ACC_PUBLIC, "com/ejemplo/A", null, "java/lang/Object", null);
        cw.visitField(ACC_PRIVATE, "fooObf", "I", null, null).visitEnd();

        MethodVisitor init = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(ALOAD, 0);
        init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        MethodVisitor bar = cw.visitMethod(ACC_PUBLIC, "barObf", "()V", null, null);
        bar.visitCode();
        bar.visitInsn(RETURN);
        bar.visitMaxs(0, 0);
        bar.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static byte[] buildSimple(String internalName) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V17, ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        MethodVisitor init = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(ALOAD, 0);
        init.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static void writeJar(Path p, Map<String, byte[]> entries) throws IOException {
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(p))) {
            for (var e : entries.entrySet()) {
                ZipEntry ze = new ZipEntry(e.getKey());
                z.putNextEntry(ze);
                z.write(e.getValue());
                z.closeEntry();
            }
        }
    }

    private static Map<String, byte[]> readAll(Path jar) throws IOException {
        Map<String, byte[]> out = new LinkedHashMap<>();
        try (ZipFile zf = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry ze = en.nextElement();
                try (var is = zf.getInputStream(ze)) {
                    out.put(ze.getName(), is.readAllBytes());
                }
            }
        }
        return out;
    }

    /** Возвращает строку со всеми именами методов и полей класса для проверки. */
    private static String disasmNames(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        new ClassReader(bytes).accept(new org.objectweb.asm.ClassVisitor(Opcodes.ASM9) {
            @Override
            public org.objectweb.asm.FieldVisitor visitField(int access, String name, String desc,
                                                             String signature, Object value) {
                sb.append("F:").append(name).append(':').append(desc).append('\n');
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                sb.append("M:").append(name).append(':').append(desc).append('\n');
                return null;
            }
        }, 0);
        return sb.toString();
    }

    // Для некоторых тестов нужен просто buffer of bytes:
    @SuppressWarnings("unused")
    private static byte[] bytes(ByteArrayOutputStream b) { return b.toByteArray(); }

    @SuppressWarnings("unused")
    private static Map<String, byte[]> map() { return new HashMap<>(); }
}
