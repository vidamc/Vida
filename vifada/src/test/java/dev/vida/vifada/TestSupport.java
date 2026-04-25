/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import static org.objectweb.asm.Opcodes.*;

import dev.vida.vifada.internal.AsmNames;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;

/** Утилиты для сборки тестовых классов через ASM на лету. */
final class TestSupport {
    private TestSupport() {}

    /** ClassLoader, загружающий классы из in-memory карты internal-name → bytes. */
    static final class ByteClassLoader extends ClassLoader {
        private final Map<String, byte[]> defs = new HashMap<>();

        ByteClassLoader(ClassLoader parent) { super(parent); }

        void put(String internalName, byte[] bytes) {
            defs.put(internalName, bytes);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] b = defs.get(name.replace('.', '/'));
            if (b == null) return super.findClass(name);
            return defineClass(name, b, 0, b.length);
        }
    }

    // =================================================================
    //                       BUILDER: TARGET CLASS
    // =================================================================

    /** Собирает простой целевой класс с одним void-методом tick() и полем counter:I. */
    static byte[] buildSimpleTarget(String internalName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V21, ACC_PUBLIC, internalName, null, "java/lang/Object", null);

        // public int counter;
        cw.visitField(ACC_PUBLIC, "counter", "I", null, null).visitEnd();

        // <init>()V
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        // public void tick() { this.counter = this.counter + 1; }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "tick", "()V", null, null);
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
        }
        // public String greet(String s) { return "hi:" + s; }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "greet",
                    "(Ljava/lang/String;)Ljava/lang/String;", null, null);
            mv.visitCode();
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitLdcInsn("hi:");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                    "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString",
                    "()Ljava/lang/String;", false);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        // public int sum(int a, int b) { return a + b; }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "sum", "(II)I", null, null);
            mv.visitCode();
            mv.visitVarInsn(ILOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitInsn(IADD);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** Целевой класс с {@code maxed(II)I}, вызывающим {@link Math#max(int, int)}. */
    static byte[] buildMathMaxTarget(String internalName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V21, ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "maxed", "(II)I", null, null);
            mv.visitCode();
            mv.visitVarInsn(ILOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "max", "(II)I", false);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** Как {@link #buildSimpleTarget}, плюс {@code bump()V} увеличивает {@code counter}. */
    static byte[] buildTargetTickAndBump(String internalName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cw.visit(V21, ACC_PUBLIC, internalName, null, "java/lang/Object", null);
        cw.visitField(ACC_PUBLIC, "counter", "I", null, null).visitEnd();
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "tick", "()V", null, null);
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
        }
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "bump", "()V", null, null);
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
        }
        cw.visitEnd();
        return cw.toByteArray();
    }

    // =================================================================
    //                        BUILDER: MORPH CLASS
    // =================================================================

    /** Мини-DSL для конструкции морфов с аннотациями. */
    static final class MorphBuilder {
        private final ClassWriter cw;
        private final String internal;
        private final String target;
        private int priority = 1000;

        MorphBuilder(String internal, String targetFqn) {
            this.internal = internal;
            this.target = targetFqn;
            this.cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        }

        MorphBuilder withPriority(int p) { this.priority = p; return this; }

        MorphBuilder start() {
            cw.visit(V21, ACC_PUBLIC | ACC_ABSTRACT, internal, null, "java/lang/Object", null);
            AnnotationVisitor ma = cw.visitAnnotation(AsmNames.VIFADA_MORPH_DESC, false);
            ma.visit("target", target);
            ma.visit("priority", priority);
            ma.visitEnd();
            return this;
        }

        /** Добавляет поле, помеченное {@code @VifadaShadow}. */
        MorphBuilder shadowField(String name, String desc) {
            var fv = cw.visitField(ACC_PROTECTED, name, desc, null, null);
            var sv = fv.visitAnnotation(AsmNames.VIFADA_SHADOW_DESC, false);
            sv.visitEnd();
            fv.visitEnd();
            return this;
        }

        /**
         * Генерирует inject-метод {@code public void <methodName>(CallbackInfo ci)},
         * тело которого вызывает {@code CallbackInfo.cancel()} если {@code cancel == true},
         * и в любом случае увеличивает {@code this.<fieldName>} на 1.
         */
        MorphBuilder inject(String methodName, String targetSpec, InjectionPoint at,
                            String fieldName, boolean cancel) {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName,
                    "(" + AsmNames.CALLBACK_INFO_DESC + ")V", null, null);
            AnnotationVisitor inj = mv.visitAnnotation(AsmNames.VIFADA_INJECT_DESC, false);
            inj.visit("method", targetSpec);
            AnnotationVisitor atAnn = inj.visitAnnotation("at", AsmNames.VIFADA_AT_DESC);
            atAnn.visitEnum("value", AsmNames.INJECTION_POINT_DESC, at.name());
            atAnn.visitEnd();
            inj.visitEnd();

            mv.visitCode();

            // this.field = this.field + 1
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, internal, fieldName, "I");
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitFieldInsn(PUTFIELD, internal, fieldName, "I");

            if (cancel) {
                // ci.cancel()
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKEVIRTUAL, AsmNames.CALLBACK_INFO_INTERNAL,
                        "cancel", "()V", false);
            }
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            return this;
        }

        /** Перезаписывает метод sum(II)I так, чтобы он возвращал a - b. */
        MorphBuilder overwriteSum() {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "sum", "(II)I", null, null);
            AnnotationVisitor ov = mv.visitAnnotation(AsmNames.VIFADA_OVERWRITE_DESC, false);
            ov.visitEnd();

            mv.visitCode();
            mv.visitVarInsn(ILOAD, 1);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitInsn(ISUB);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            return this;
        }

        /**
         * {@code @VifadaMulti} — один хендлер {@code (CallbackInfo)V} на несколько void-методов.
         */
        MorphBuilder injectMulti(String methodName, String[] targetSpecs, InjectionPoint at,
                                 String fieldName, boolean cancel) {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, methodName,
                    "(" + AsmNames.CALLBACK_INFO_DESC + ")V", null, null);
            AnnotationVisitor multi = mv.visitAnnotation(AsmNames.VIFADA_MULTI_DESC, false);
            AnnotationVisitor arr = multi.visitArray("methods");
            for (String s : targetSpecs) {
                arr.visit(null, s);
            }
            arr.visitEnd();
            AnnotationVisitor atAnn = multi.visitAnnotation("at", AsmNames.VIFADA_AT_DESC);
            atAnn.visitEnum("value", AsmNames.INJECTION_POINT_DESC, at.name());
            atAnn.visitEnd();
            multi.visitEnd();

            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, internal, fieldName, "I");
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
            mv.visitFieldInsn(PUTFIELD, internal, fieldName, "I");
            if (cancel) {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKEVIRTUAL, AsmNames.CALLBACK_INFO_INTERNAL,
                        "cancel", "()V", false);
            }
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            return this;
        }

        /** Статический редирект {@link Math#max} на сложение. */
        MorphBuilder redirectMathMax(String redirectMethodName) {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, redirectMethodName,
                    "(II)I", null, null);
            AnnotationVisitor rd = mv.visitAnnotation(AsmNames.VIFADA_REDIRECT_DESC, false);
            rd.visit("method", "maxed(II)I");
            rd.visit("invokeOwner", "java/lang/Math");
            rd.visit("invokeName", "max");
            rd.visit("invokeDescriptor", "(II)I");
            rd.visitEnd();
            mv.visitCode();
            mv.visitVarInsn(ILOAD, 0);
            mv.visitVarInsn(ILOAD, 1);
            mv.visitInsn(IADD);
            mv.visitInsn(IRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
            return this;
        }

        /** Возвращает собранные байты. */
        byte[] build() {
            cw.visitEnd();
            return cw.toByteArray();
        }
    }
}
