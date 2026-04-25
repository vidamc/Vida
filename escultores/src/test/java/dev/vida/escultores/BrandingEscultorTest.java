/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.escultores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;

final class BrandingEscultorTest {

    private static final Handle INDY_BSM = new Handle(
            H_INVOKESTATIC,
            "java/lang/invoke/StringConcatFactory",
            "makeConcatWithConstants",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                    + "Ljava/lang/invoke/MethodType;Ljava/lang/String;"
                    + "[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
            false);

    /** Собирает класс с одной константой-строкой — имитируем DebugHud. */
    private static byte[] buildClassWithStringConst(String internalName, String value) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V21, ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // public String getLine() { return "<value>"; }
        mv = cw.visitMethod(ACC_PUBLIC, "getLine", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitLdcInsn(value);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static List<String> stringConstants(byte[] classfile) {
        ClassNode cn = new ClassNode();
        new ClassReader(classfile).accept(cn, 0);
        List<String> out = new ArrayList<>();
        cn.methods.forEach(mn -> {
            for (AbstractInsnNode i : mn.instructions) {
                if (i instanceof LdcInsnNode ldc && ldc.cst instanceof String s) {
                    out.add(s);
                }
            }
        });
        return out;
    }

    @Test
    void might_match_on_f3_format() {
        byte[] cls = buildClassWithStringConst("x/DebugHud", "Minecraft %s (%s/%s%s)");
        assertThat(BrandingEscultor.mightMatch(cls)).isTrue();
    }

    @Test
    void might_not_match_on_unrelated_class() {
        byte[] cls = buildClassWithStringConst("x/Foo", "hello world");
        assertThat(BrandingEscultor.mightMatch(cls)).isFalse();
    }

    @Test
    void patches_primary_format_string() {
        byte[] cls = buildClassWithStringConst("x/DebugHud", "Minecraft %s (%s/%s%s)");
        byte[] patched = BrandingEscultor.tryPatch(cls);
        assertThat(patched).isNotNull().isNotEqualTo(cls);
        assertThat(stringConstants(patched)).contains("Minecraft %1$s (vida)");
        assertThat(stringConstants(patched)).doesNotContain("Minecraft %s (%s/%s%s)");
    }

    @Test
    void patches_format_with_extra_hint() {
        byte[] cls = buildClassWithStringConst("x/DebugHud",
                "Minecraft %s (%s/%s%s, %s)");
        byte[] patched = BrandingEscultor.tryPatch(cls);
        assertThat(patched).isNotNull();
        assertThat(stringConstants(patched)).contains("Minecraft %1$s (vida)");
    }

    @Test
    void does_not_patch_similar_but_unrelated_strings() {
        byte[] cls = buildClassWithStringConst("x/Logger", "Starting Minecraft %s now");
        assertThat(BrandingEscultor.mightMatch(cls)).isFalse();
        assertThat(BrandingEscultor.tryPatch(cls)).isNull();
    }

    @Test
    void does_not_patch_log_message_with_no_slash() {
        byte[] cls = buildClassWithStringConst("x/Foo", "Minecraft %s (loading)");
        byte[] patched = BrandingEscultor.tryPatch(cls);
        assertThat(patched).isNull();
    }

    @Test
    void replacement_format_produces_expected_output() {
        String replaced = String.format("Minecraft %1$s (vida)",
                "1.21.1", "vida-1.21.1-0.1.0-SNAPSHOT", "vanilla", "");
        assertThat(replaced).isEqualTo("Minecraft 1.21.1 (vida)");
    }

    private static byte[] buildClassWithIndyConcat(
            String internalName, String template, int argCount) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V21, ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        String methodDesc = "(" + "Ljava/lang/String;".repeat(argCount) + ")Ljava/lang/String;";
        mv = cw.visitMethod(ACC_PUBLIC, "getLine", methodDesc, null, null);
        mv.visitCode();
        for (int i = 0; i < argCount; i++) {
            mv.visitVarInsn(ALOAD, i + 1);
        }
        mv.visitInvokeDynamicInsn("makeConcatWithConstants", methodDesc, INDY_BSM, template);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    @Test
    void might_match_on_indy_concat_template() {
        byte[] cls = buildClassWithIndyConcat(
                "x/DebugHud21", "Minecraft \u0001 (\u0001/\u0001)", 3);
        assertThat(BrandingEscultor.mightMatch(cls)).isTrue();
    }

    @Test
    void might_not_match_on_indy_unrelated_template() {
        byte[] cls = buildClassWithIndyConcat("x/Other", "Loading \u0001 mods", 1);
        assertThat(BrandingEscultor.mightMatch(cls)).isFalse();
    }

    @Test
    void patches_indy_concat_to_vida_brand() {
        byte[] cls = buildClassWithIndyConcat(
                "x/DebugHud21b", "Minecraft \u0001 (\u0001/\u0001)", 3);
        byte[] patched = BrandingEscultor.tryPatch(cls);
        assertThat(patched).isNotNull().isNotEqualTo(cls);
    }

    @Test
    void patched_indy_class_is_jvm_loadable_and_returns_vida_brand() throws Exception {
        byte[] cls = buildClassWithIndyConcat(
                "x/DebugHud21c", "Minecraft \u0001 (\u0001/\u0001)", 3);
        byte[] patched = BrandingEscultor.tryPatch(cls);
        assertThat(patched).isNotNull();

        ClassLoader cl = new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if ("x.DebugHud21c".equals(name)) {
                    return defineClass(name, patched, 0, patched.length);
                }
                return super.findClass(name);
            }
        };
        Class<?> k = cl.loadClass("x.DebugHud21c");
        Object inst = k.getDeclaredConstructor().newInstance();
        Object out = k.getMethod("getLine", String.class, String.class, String.class)
                .invoke(inst, "1.21.1", "1.21.1-launcher", "vanilla");
        assertThat(out).isEqualTo("Minecraft 1.21.1 (vida)");
    }

    @Test
    void is_f3_concat_template_recognises_variants() {
        assertThat(BrandingEscultor.isF3ConcatTemplate(
                "Minecraft \u0001 (\u0001/\u0001)")).isTrue();
        assertThat(BrandingEscultor.isF3ConcatTemplate(
                "Minecraft \u0001 (\u0001/\u0001, \u0001)")).isTrue();
        assertThat(BrandingEscultor.isF3ConcatTemplate(
                "Minecraft \u0001 (loading)")).isTrue();
        assertThat(BrandingEscultor.isF3ConcatTemplate(
                "Loading \u0001 mods")).isFalse();
        assertThat(BrandingEscultor.isF3ConcatTemplate(null)).isFalse();
    }

    @Test
    void patched_class_is_jvm_loadable_with_branches() throws Exception {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V21, ACC_PUBLIC, "x/DebugFmt", null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "format",
                "(ZLjava/lang/String;)Ljava/lang/String;", null, null);
        mv.visitCode();
        org.objectweb.asm.Label elseLabel = new org.objectweb.asm.Label();
        org.objectweb.asm.Label endLabel  = new org.objectweb.asm.Label();
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IFEQ, elseLabel);
        mv.visitLdcInsn("Minecraft %s (%s/%s%s)");
        mv.visitInsn(ICONST_1);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(AASTORE);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "format",
                "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false);
        mv.visitJumpInsn(GOTO, endLabel);
        mv.visitLabel(elseLabel);
        mv.visitLdcInsn("no-flag");
        mv.visitLabel(endLabel);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        byte[] orig = cw.toByteArray();
        byte[] patched = BrandingEscultor.tryPatch(orig);
        assertThat(patched).isNotNull();

        ClassLoader cl = new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if ("x.DebugFmt".equals(name)) {
                    return defineClass(name, patched, 0, patched.length);
                }
                return super.findClass(name);
            }
        };
        Class<?> k = cl.loadClass("x.DebugFmt");
        Object inst = k.getDeclaredConstructor().newInstance();
        Object out = k.getMethod("format", boolean.class, String.class)
                .invoke(inst, true, "1.21.1");
        assertThat(out).isEqualTo("Minecraft 1.21.1 (vida)");
    }
}
