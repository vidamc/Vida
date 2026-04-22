/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import static org.objectweb.asm.Opcodes.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

/** Утилиты для генерации тестовых JAR-ов с модом/морфом. */
final class TestSupport {

    private TestSupport() {}

    private static final String VIFADA_MORPH_DESC    = "Ldev/vida/vifada/VifadaMorph;";
    private static final String VIFADA_INJECT_DESC   = "Ldev/vida/vifada/VifadaInject;";
    private static final String VIFADA_AT_DESC       = "Ldev/vida/vifada/VifadaAt;";
    private static final String VIFADA_SHADOW_DESC   = "Ldev/vida/vifada/VifadaShadow;";
    private static final String INJECTION_POINT_DESC = "Ldev/vida/vifada/InjectionPoint;";
    private static final String CALLBACK_INFO_DESC   = "Ldev/vida/vifada/CallbackInfo;";

    // ================================================================
    //                          MANIFEST JSON
    // ================================================================

    /** Собирает минимальный {@code vida.mod.json}. */
    static String mod(String id, String version) {
        return """
                {
                  "schema": 1,
                  "id": "%s",
                  "version": "%s",
                  "name": "%s",
                  "environment": "universal"
                }
                """.formatted(id, version, id);
    }

    /**
     * Собирает {@code vida.mod.json} с произвольным блоком
     * {@code dependencies.required} (ключ → SemVer range).
     */
    static String modWithRequired(String id, String version, Map<String, String> required) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\n  \"schema\": 1,\n  \"id\": \"").append(id)
          .append("\",\n  \"version\": \"").append(version)
          .append("\",\n  \"name\": \"").append(id)
          .append("\",\n  \"environment\": \"universal\",\n  \"dependencies\": {\n    \"required\": {");
        boolean first = true;
        for (var e : required.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\n      \"").append(e.getKey()).append("\": \"").append(e.getValue()).append("\"");
            first = false;
        }
        sb.append("\n    }\n  }\n}\n");
        return sb.toString();
    }

    /** Как {@link #buildModJar}, но с заданным сырым JSON-манифестом. */
    static Path buildModJarRaw(Path target, String manifestJson,
                               byte[] morphClassOpt, String morphInternalNameOpt) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("vida.mod.json", manifestJson.getBytes(StandardCharsets.UTF_8));
        if (morphClassOpt != null) {
            entries.put(morphInternalNameOpt + ".class", morphClassOpt);
        }
        return writeJar(target, entries);
    }

    // ================================================================
    //                            JAR BUILDER
    // ================================================================

    /** Пишет JAR с набором entry-файлов. */
    static Path writeJar(Path target, Map<String, byte[]> entries) throws IOException {
        Files.createDirectories(target.getParent());
        try (OutputStream os = Files.newOutputStream(target);
             JarOutputStream jos = new JarOutputStream(os)) {
            for (var e : entries.entrySet()) {
                JarEntry je = new JarEntry(e.getKey());
                jos.putNextEntry(je);
                jos.write(e.getValue());
                jos.closeEntry();
            }
        }
        return target;
    }

    /** Собирает простой мод-JAR с заданным манифестом и опциональным морфом. */
    static Path buildModJar(Path target, String id, String version,
                            byte[] morphClassOpt, String morphInternalNameOpt) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("vida.mod.json", mod(id, version).getBytes(StandardCharsets.UTF_8));
        if (morphClassOpt != null) {
            entries.put(morphInternalNameOpt + ".class", morphClassOpt);
        }
        return writeJar(target, entries);
    }

    // ================================================================
    //                            CLASS BUILDERS
    // ================================================================

    /** Простой целевой класс, описанный в {@code vifada.TestSupport}, воспроизведён для автономии теста. */
    static byte[] buildSimpleTarget(String internalName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V21, ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        cw.visitField(ACC_PUBLIC, "counter", "I", null, null).visitEnd();

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "tick", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, internalName, "counter", "I");
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IADD);
        mv.visitFieldInsn(PUTFIELD, internalName, "counter", "I");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    /** Морф с одним HEAD-инъектом в {@code tick()V}, поднимающим {@code counter}. */
    static byte[] buildHeadInjectMorph(String internalName, String targetFqn) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V21, ACC_PUBLIC | ACC_ABSTRACT, internalName, null, "java/lang/Object", null);

        // @VifadaMorph(target="<targetFqn>")
        AnnotationVisitor av = cw.visitAnnotation(VIFADA_MORPH_DESC, false);
        av.visit("target", targetFqn);
        av.visitEnd();

        // @VifadaShadow protected int counter
        var fv = cw.visitField(ACC_PROTECTED, "counter", "I", null, null);
        fv.visitAnnotation(VIFADA_SHADOW_DESC, false).visitEnd();
        fv.visitEnd();

        // public void preTick(CallbackInfo ci) { this.counter++; }
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "preTick",
                "(" + CALLBACK_INFO_DESC + ")V", null, null);
        AnnotationVisitor ia = mv.visitAnnotation(VIFADA_INJECT_DESC, false);
        ia.visit("method", "tick()V");
        AnnotationVisitor at = ia.visitAnnotation("at", VIFADA_AT_DESC);
        at.visitEnum("value", INJECTION_POINT_DESC, "HEAD");
        at.visitEnd();
        ia.visitEnd();

        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, internalName, "counter", "I");
        mv.visitInsn(ICONST_1);
        mv.visitInsn(IADD);
        mv.visitFieldInsn(PUTFIELD, internalName, "counter", "I");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    /** Плоский класс без морф-аннотаций. */
    static byte[] buildPlainClass(String internalName) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(V21, ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        cw.visitEnd();
        return cw.toByteArray();
    }
}
