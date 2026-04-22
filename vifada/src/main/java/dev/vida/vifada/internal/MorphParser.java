/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada.internal;

import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import dev.vida.vifada.InjectionPoint;
import dev.vida.vifada.VifadaError;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Парсер байткода морфа в {@link MorphDescriptor}.
 *
 * <p>Читает аннотации {@code @VifadaMorph}, {@code @VifadaInject},
 * {@code @VifadaOverwrite}, {@code @VifadaShadow} и собирает дескрипторы
 * для последующего применения.
 */
@ApiStatus.Internal
public final class MorphParser {

    private MorphParser() {}

    public static Result<MorphDescriptor, VifadaError> parse(String morphName, byte[] bytes) {
        ClassNode node;
        try {
            ClassReader reader = new ClassReader(bytes);
            node = new ClassNode(AsmNames.API);
            reader.accept(node, ClassReader.SKIP_FRAMES);
        } catch (RuntimeException ex) {
            return Result.err(new VifadaError.AsmFailure(morphName,
                    "failed to read morph class: " + ex.getClass().getSimpleName()
                            + ": " + ex.getMessage()));
        }

        AnnotationNode morphAnn = findAnnotation(node.visibleAnnotations, AsmNames.VIFADA_MORPH_DESC);
        if (morphAnn == null) {
            morphAnn = findAnnotation(node.invisibleAnnotations, AsmNames.VIFADA_MORPH_DESC);
        }
        if (morphAnn == null) {
            return Result.err(new VifadaError.NotAMorph(morphName));
        }

        String target = (String) annotationValue(morphAnn, "target");
        if (target == null || target.isBlank()) {
            return Result.err(new VifadaError.BadMorph(morphName,
                    "@VifadaMorph.target must be set"));
        }
        Integer priorityVal = (Integer) annotationValue(morphAnn, "priority");
        int priority = priorityVal == null ? 1000 : priorityVal;

        String targetInternal = target.replace('.', '/');
        MorphDescriptor desc = new MorphDescriptor(node.name, targetInternal, priority, node);

        // ---- Fields: только shadow-поля ------------------------------------
        if (node.fields != null) {
            for (FieldNode f : node.fields) {
                AnnotationNode a = findAnnotation(f.visibleAnnotations, AsmNames.VIFADA_SHADOW_DESC);
                if (a == null) a = findAnnotation(f.invisibleAnnotations, AsmNames.VIFADA_SHADOW_DESC);
                if (a != null) {
                    boolean silent = boolOr(annotationValue(a, "silentMissing"), false);
                    desc.shadows.add(new ShadowDescriptor(
                            ShadowDescriptor.Kind.FIELD, f.name, f.desc, silent));
                }
            }
        }

        // ---- Methods: inject / overwrite / shadow --------------------------
        if (node.methods != null) {
            for (MethodNode m : node.methods) {
                VifadaError err = parseMethod(morphName, desc, m);
                if (err != null) return Result.err(err);
            }
        }

        return Result.ok(desc);
    }

    private static VifadaError parseMethod(String morphName, MorphDescriptor desc, MethodNode m) {
        AnnotationNode shadowA = findAnnotation(m.visibleAnnotations, AsmNames.VIFADA_SHADOW_DESC);
        if (shadowA == null) shadowA = findAnnotation(m.invisibleAnnotations, AsmNames.VIFADA_SHADOW_DESC);
        if (shadowA != null) {
            boolean silent = boolOr(annotationValue(shadowA, "silentMissing"), false);
            desc.shadows.add(new ShadowDescriptor(
                    ShadowDescriptor.Kind.METHOD, m.name, m.desc, silent));
            return null;
        }

        AnnotationNode overwriteA = findAnnotation(m.visibleAnnotations, AsmNames.VIFADA_OVERWRITE_DESC);
        if (overwriteA == null) {
            overwriteA = findAnnotation(m.invisibleAnnotations, AsmNames.VIFADA_OVERWRITE_DESC);
        }
        if (overwriteA != null) {
            String methodSpec = (String) annotationValue(overwriteA, "method");
            if (methodSpec == null || methodSpec.isEmpty()) {
                methodSpec = m.name + m.desc;
            }
            boolean silent = boolOr(annotationValue(overwriteA, "silentMissing"), false);
            desc.overwrites.add(new OverwriteDescriptor(m, methodSpec, silent));
            return null;
        }

        AnnotationNode injectA = findAnnotation(m.visibleAnnotations, AsmNames.VIFADA_INJECT_DESC);
        if (injectA == null) {
            injectA = findAnnotation(m.invisibleAnnotations, AsmNames.VIFADA_INJECT_DESC);
        }
        if (injectA != null) {
            String methodSpec = (String) annotationValue(injectA, "method");
            if (methodSpec == null || methodSpec.isBlank()) {
                return new VifadaError.BadMorph(morphName,
                        "@VifadaInject on '" + m.name + m.desc + "' must specify method=");
            }
            AnnotationNode atAnn = (AnnotationNode) annotationValue(injectA, "at");
            if (atAnn == null) {
                return new VifadaError.BadMorph(morphName,
                        "@VifadaInject on '" + m.name + m.desc + "' must specify at=@VifadaAt(...)");
            }
            InjectionPoint ip = enumValue(atAnn, "value", InjectionPoint.class);
            if (ip == null) {
                return new VifadaError.BadMorph(morphName,
                        "@VifadaAt.value is required for inject '" + m.name + "'");
            }
            boolean requireTarget = boolOr(annotationValue(injectA, "requireTarget"), true);
            desc.injects.add(new InjectDescriptor(m, methodSpec, ip, requireTarget));
        }
        return null;
    }

    // ===================================================================
    //                        ANNOTATION HELPERS
    // ===================================================================

    static AnnotationNode findAnnotation(List<AnnotationNode> xs, String desc) {
        if (xs == null) return null;
        for (AnnotationNode a : xs) {
            if (desc.equals(a.desc)) return a;
        }
        return null;
    }

    static Object annotationValue(AnnotationNode a, String name) {
        if (a == null || a.values == null) return null;
        for (int i = 0; i + 1 < a.values.size(); i += 2) {
            if (name.equals(a.values.get(i))) {
                return a.values.get(i + 1);
            }
        }
        return null;
    }

    /** Читает значение enum-атрибута аннотации. В ASM это {@code String[]{desc, name}}. */
    static <E extends Enum<E>> E enumValue(AnnotationNode a, String attr, Class<E> enumType) {
        Object v = annotationValue(a, attr);
        if (!(v instanceof String[] arr) || arr.length < 2) return null;
        String valueName = arr[1];
        for (E e : enumType.getEnumConstants()) {
            if (e.name().equals(valueName)) return e;
        }
        return null;
    }

    private static boolean boolOr(Object v, boolean dflt) {
        return v instanceof Boolean b ? b : dflt;
    }
}
