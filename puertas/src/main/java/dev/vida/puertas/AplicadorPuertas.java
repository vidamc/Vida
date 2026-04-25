/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.puertas;

import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Применяет {@link PuertaDirectiva}-директивы к байт-коду одного класса.
 *
 * <p>Работа:
 * <ol>
 *   <li>{@link ClassReader} — читаем класс в ASM tree-модель;</li>
 *   <li>для каждого relevant-объявления правим флаги доступа в узле;</li>
 *   <li>{@link ClassWriter} — пишем обратно.</li>
 * </ol>
 *
 * <p>Если ни одна директива не затрагивает целевой класс, возвращаются
 * оригинальные байты (без ре-кодирования) — это быстрее и меньше шума
 * в диффах.
 *
 * <p>Класс иммутабельный — конкретные директивы передаются в метод
 * трансформации; применять можно к разным классам параллельно.
 */
@ApiStatus.Stable
public final class AplicadorPuertas {

    private static final Log LOG = Log.of(AplicadorPuertas.class);

    /** ASM API version (соответствует :vifada). */
    private static final int ASM_API = Opcodes.ASM9;

    /** Маска visibility для быстрой подмены на public. */
    private static final int MASCARA_VISIBILIDAD =
            ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);

    private AplicadorPuertas() {}

    /**
     * Индекс директив по {@link PuertaDirectiva#claseInternal()} — для jar-in-jar и
     * многопроходной обработки без сканирования всего списка на каждый класс.
     */
    public static Map<String, List<PuertaDirectiva>> indicePorClase(List<PuertaDirectiva> todas) {
        Objects.requireNonNull(todas, "todas");
        Map<String, List<PuertaDirectiva>> map = new LinkedHashMap<>();
        for (PuertaDirectiva d : todas) {
            map.computeIfAbsent(d.claseInternal(), k -> new ArrayList<>()).add(d);
        }
        for (Map.Entry<String, List<PuertaDirectiva>> e : map.entrySet()) {
            e.setValue(List.copyOf(e.getValue()));
        }
        return Map.copyOf(map);
    }

    /** Результат применения — новые байты + отчёт. */
    public record Resultado(byte[] bytes, Informe informe) {}

    /**
     * Отчёт: что удалось применить, и что было пропущено из-за того,
     * что целевой член не найден.
     *
     * @param aplicadas количество успешно применённых директив
     * @param perdidas  директивы, для которых не нашёлся член в целевом классе
     */
    public record Informe(int aplicadas, List<PuertaDirectiva> perdidas) {
        public Informe {
            perdidas = List.copyOf(Objects.requireNonNull(perdidas, "perdidas"));
        }
    }

    /**
     * Применяет все директивы, относящиеся к данному классу, и возвращает
     * новый jvm-байт-код. Если ни одна директива не подходит — возвращает
     * исходные байты как есть.
     *
     * @param bytes     исходный байткод класса
     * @param todas     все известные директивы (будет выбрано подмножество
     *                  {@code claseInternal == текущий класс})
     */
    public static Resultado aplicar(byte[] bytes, List<PuertaDirectiva> todas) {
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(todas, "todas");

        ClassReader cr = new ClassReader(bytes);
        String internal = cr.getClassName();

        List<PuertaDirectiva> relevantes = new ArrayList<>();
        for (PuertaDirectiva d : todas) {
            if (d.objetivoEs(internal)) relevantes.add(d);
        }
        if (relevantes.isEmpty()) {
            return new Resultado(bytes, new Informe(0, List.of()));
        }

        ClassNode cn = new ClassNode(ASM_API);
        cr.accept(cn, 0);

        List<PuertaDirectiva> perdidas = new ArrayList<>();
        int aplicadas = 0;
        for (PuertaDirectiva d : relevantes) {
            boolean ok = switch (d.objetivo()) {
                case CLASE  -> aplicarClase(cn, d);
                case METODO -> aplicarMetodo(cn, d);
                case CAMPO  -> aplicarCampo(cn, d);
            };
            if (ok) aplicadas++;
            else perdidas.add(d);
        }

        if (aplicadas == 0) {
            // Ничего не изменилось — вернём оригинал.
            return new Resultado(bytes, new Informe(0, perdidas));
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        byte[] nuevo = cw.toByteArray();
        LOG.debug("Puertas применены: class={} — {} директив", internal, aplicadas);
        return new Resultado(nuevo, new Informe(aplicadas, perdidas));
    }

    // ------------------------------------------------------------------ internals

    private static boolean aplicarClase(ClassNode cn, PuertaDirectiva d) {
        switch (d.accion()) {
            case ACCESIBLE -> cn.access = aPublic(cn.access);
            case EXTENSIBLE -> cn.access = quitarFinal(cn.access);
            case MUTABLE -> { /* не должно приходить — отлавливается парсером */ }
        }
        return true;
    }

    private static boolean aplicarMetodo(ClassNode cn, PuertaDirectiva d) {
        String name = d.nombreMiembro().orElseThrow();
        String desc = d.descriptor().orElseThrow();
        MethodNode mn = buscarMetodo(cn, name, desc);
        if (mn == null) return false;
        switch (d.accion()) {
            case ACCESIBLE  -> mn.access = aPublic(mn.access);
            case EXTENSIBLE -> mn.access = quitarFinal(mn.access);
            case MUTABLE    -> { /* не приходит для method */ }
        }
        return true;
    }

    private static boolean aplicarCampo(ClassNode cn, PuertaDirectiva d) {
        String name = d.nombreMiembro().orElseThrow();
        String desc = d.descriptor().orElseThrow();
        FieldNode fn = buscarCampo(cn, name, desc);
        if (fn == null) return false;
        switch (d.accion()) {
            case ACCESIBLE -> {
                fn.access = aPublic(fn.access);
                // Fabric AW / Forge AT: accesible для полей снимает также final.
                fn.access = quitarFinal(fn.access);
            }
            case MUTABLE    -> fn.access = quitarFinal(fn.access);
            case EXTENSIBLE -> { /* не имеет смысла для поля — парсер не пропустит */ }
        }
        return true;
    }

    private static MethodNode buscarMetodo(ClassNode cn, String name, String desc) {
        if (cn.methods == null) return null;
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals(name) && mn.desc.equals(desc)) return mn;
        }
        return null;
    }

    private static FieldNode buscarCampo(ClassNode cn, String name, String desc) {
        if (cn.fields == null) return null;
        for (FieldNode fn : cn.fields) {
            if (fn.name.equals(name) && fn.desc.equals(desc)) return fn;
        }
        return null;
    }

    private static int aPublic(int access) {
        return (access & MASCARA_VISIBILIDAD) | Opcodes.ACC_PUBLIC;
    }

    private static int quitarFinal(int access) {
        return access & ~Opcodes.ACC_FINAL;
    }
}
