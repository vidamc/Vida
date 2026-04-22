/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.puertas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Проверяет ASM-применение директив к синтетическому классу.
 *
 * <p>Мы не используем reflection на Minecraft-классы — собираем свой
 * компакт {@code net/ejemplo/Sujeto} с private final полем, final
 * методом и package-private классом, затем через {@link AplicadorPuertas}
 * убеждаемся, что флаги доступа меняются ожидаемо.
 */
class AplicadorPuertasTest {

    private static final String NOMBRE = "net/ejemplo/Sujeto";

    /** Генерирует class-file: private final class Sujeto { private final int count; private final void hacer() {} }. */
    private static byte[] generarSujeto() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        // package-private + final
        cw.visit(Opcodes.V21, Opcodes.ACC_FINAL, NOMBRE, null, "java/lang/Object", null);

        // private final int count;
        FieldVisitor fv = cw.visitField(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "count", "I", null, null);
        fv.visitEnd();

        // private final void hacer() {}
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "hacer", "()V", null, null);
        mv.visitCode();
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // конструктор (необязателен для теста, но без него байткод странный)
        MethodVisitor init = cw.visitMethod(0, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(0, 0);
        init.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private static ClassNode leer(byte[] b) {
        ClassNode cn = new ClassNode(Opcodes.ASM9);
        new ClassReader(b).accept(cn, 0);
        return cn;
    }

    private static MethodNode metodo(ClassNode cn, String name) {
        for (MethodNode m : cn.methods) if (m.name.equals(name)) return m;
        throw new AssertionError("метод не найден: " + name);
    }

    private static FieldNode campo(ClassNode cn, String name) {
        for (FieldNode f : cn.fields) if (f.name.equals(name)) return f;
        throw new AssertionError("поле не найдено: " + name);
    }

    private static PuertaDirectiva d(Accion a, Objetivo o, String cls, String m, String desc) {
        return new PuertaDirectiva(a, o, cls,
                m == null ? Optional.empty() : Optional.of(m),
                desc == null ? Optional.empty() : Optional.of(desc),
                1);
    }

    @Test
    void accesible_class_anade_public_quita_final() {
        var res = AplicadorPuertas.aplicar(generarSujeto(),
                List.of(d(Accion.ACCESIBLE, Objetivo.CLASE, NOMBRE, null, null)));
        ClassNode cn = leer(res.bytes());
        assertThat((cn.access & Opcodes.ACC_PUBLIC) != 0).isTrue();
        // accesible НЕ снимает final у класса (для этого есть extensible)
        assertThat((cn.access & Opcodes.ACC_FINAL) != 0).isTrue();
        assertThat(res.informe().aplicadas()).isEqualTo(1);
    }

    @Test
    void extensible_class_quita_final() {
        var res = AplicadorPuertas.aplicar(generarSujeto(),
                List.of(d(Accion.EXTENSIBLE, Objetivo.CLASE, NOMBRE, null, null)));
        ClassNode cn = leer(res.bytes());
        assertThat((cn.access & Opcodes.ACC_FINAL) != 0).isFalse();
    }

    @Test
    void accesible_field_hace_public_y_quita_final() {
        var res = AplicadorPuertas.aplicar(generarSujeto(),
                List.of(d(Accion.ACCESIBLE, Objetivo.CAMPO, NOMBRE, "count", "I")));
        FieldNode f = campo(leer(res.bytes()), "count");
        assertThat((f.access & Opcodes.ACC_PUBLIC) != 0).isTrue();
        assertThat((f.access & Opcodes.ACC_PRIVATE) != 0).isFalse();
        assertThat((f.access & Opcodes.ACC_FINAL) != 0).isFalse();
    }

    @Test
    void mutable_field_quita_final_conserva_private() {
        var res = AplicadorPuertas.aplicar(generarSujeto(),
                List.of(d(Accion.MUTABLE, Objetivo.CAMPO, NOMBRE, "count", "I")));
        FieldNode f = campo(leer(res.bytes()), "count");
        assertThat((f.access & Opcodes.ACC_FINAL) != 0).isFalse();
        assertThat((f.access & Opcodes.ACC_PRIVATE) != 0).isTrue();
    }

    @Test
    void accesible_method_hace_public_no_final() {
        var res = AplicadorPuertas.aplicar(generarSujeto(),
                List.of(d(Accion.ACCESIBLE, Objetivo.METODO, NOMBRE, "hacer", "()V")));
        MethodNode m = metodo(leer(res.bytes()), "hacer");
        assertThat((m.access & Opcodes.ACC_PUBLIC) != 0).isTrue();
        assertThat((m.access & Opcodes.ACC_PRIVATE) != 0).isFalse();
        // final у метода при accesible НЕ снимается
        assertThat((m.access & Opcodes.ACC_FINAL) != 0).isTrue();
    }

    @Test
    void extensible_method_quita_final() {
        var res = AplicadorPuertas.aplicar(generarSujeto(),
                List.of(d(Accion.EXTENSIBLE, Objetivo.METODO, NOMBRE, "hacer", "()V")));
        MethodNode m = metodo(leer(res.bytes()), "hacer");
        assertThat((m.access & Opcodes.ACC_FINAL) != 0).isFalse();
    }

    @Test
    void no_directivas_devuelve_bytes_originales() {
        byte[] orig = generarSujeto();
        var res = AplicadorPuertas.aplicar(orig, List.of());
        assertThat(res.bytes()).isSameAs(orig);
        assertThat(res.informe().aplicadas()).isZero();
        assertThat(res.informe().perdidas()).isEmpty();
    }

    @Test
    void directiva_para_otra_clase_no_reescribe() {
        byte[] orig = generarSujeto();
        var res = AplicadorPuertas.aplicar(orig,
                List.of(d(Accion.ACCESIBLE, Objetivo.CLASE, "otra/Clase", null, null)));
        assertThat(res.bytes()).isSameAs(orig);
    }

    @Test
    void miembro_no_encontrado_reportado_como_perdido() {
        var res = AplicadorPuertas.aplicar(generarSujeto(),
                List.of(d(Accion.ACCESIBLE, Objetivo.CAMPO, NOMBRE, "noExiste", "I")));
        assertThat(res.informe().aplicadas()).isZero();
        assertThat(res.informe().perdidas()).hasSize(1);
    }

    @Test
    void multiples_directivas_aplicadas_atomicamente() {
        var res = AplicadorPuertas.aplicar(generarSujeto(), List.of(
                d(Accion.ACCESIBLE, Objetivo.CLASE, NOMBRE, null, null),
                d(Accion.EXTENSIBLE, Objetivo.CLASE, NOMBRE, null, null),
                d(Accion.MUTABLE, Objetivo.CAMPO, NOMBRE, "count", "I"),
                d(Accion.ACCESIBLE, Objetivo.METODO, NOMBRE, "hacer", "()V")
        ));
        ClassNode cn = leer(res.bytes());
        assertThat((cn.access & Opcodes.ACC_PUBLIC) != 0).isTrue();
        assertThat((cn.access & Opcodes.ACC_FINAL) != 0).isFalse();
        assertThat((campo(cn, "count").access & Opcodes.ACC_FINAL) != 0).isFalse();
        assertThat((metodo(cn, "hacer").access & Opcodes.ACC_PUBLIC) != 0).isTrue();
        assertThat(res.informe().aplicadas()).isEqualTo(4);
    }
}
