/*

 * Copyright 2026 The Vida Project Authors.

 * Licensed under the Apache License, Version 2.0.

 */

package dev.vida.vifada.internal;



import dev.vida.core.ApiStatus;

import dev.vida.core.Result;

import dev.vida.vifada.InjectionPoint;

import dev.vida.vifada.VifadaError;

import java.util.ArrayList;

import java.util.List;

import org.objectweb.asm.ClassReader;

import org.objectweb.asm.Type;

import org.objectweb.asm.tree.AnnotationNode;

import org.objectweb.asm.tree.ClassNode;

import org.objectweb.asm.tree.FieldNode;

import org.objectweb.asm.tree.MethodNode;



/**

 * Парсер байткода морфа в {@link MorphDescriptor}.

 *

 * <p>Читает аннотации {@code @VifadaMorph}, {@code @VifadaInject},

 * {@code @VifadaOverwrite}, {@code @VifadaShadow}, {@code @VifadaMulti},

 * {@code @VifadaRedirect} и собирает дескрипторы для последующего применения.

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

                if (a == null) {

                    a = findAnnotation(f.invisibleAnnotations, AsmNames.VIFADA_SHADOW_DESC);

                }

                if (a != null) {

                    boolean silent = boolOr(annotationValue(a, "silentMissing"), false);

                    desc.shadows.add(new ShadowDescriptor(

                            ShadowDescriptor.Kind.FIELD, f.name, f.desc, silent));

                }

            }

        }



        // ---- Methods ---------------------------------------------------------

        if (node.methods != null) {

            for (MethodNode m : node.methods) {

                VifadaError err = parseMethod(morphName, desc, m);

                if (err != null) {

                    return Result.err(err);

                }

            }

        }



        return Result.ok(desc);

    }



    private static VifadaError parseMethod(String morphName, MorphDescriptor desc, MethodNode m) {

        AnnotationNode shadowA = findAnnotation(m.visibleAnnotations, AsmNames.VIFADA_SHADOW_DESC);

        if (shadowA == null) {

            shadowA = findAnnotation(m.invisibleAnnotations, AsmNames.VIFADA_SHADOW_DESC);

        }

        if (shadowA != null) {

            boolean silent = boolOr(annotationValue(shadowA, "silentMissing"), false);

            desc.shadows.add(new ShadowDescriptor(

                    ShadowDescriptor.Kind.METHOD, m.name, m.desc, silent));

            return null;

        }



        boolean hasMulti = annotationPresent(m, AsmNames.VIFADA_MULTI_DESC);

        boolean hasRedirect = annotationPresent(m, AsmNames.VIFADA_REDIRECT_DESC);

        boolean hasOverwrite = annotationPresent(m, AsmNames.VIFADA_OVERWRITE_DESC);

        boolean hasInject = annotationPresent(m, AsmNames.VIFADA_INJECT_DESC);

        int roles = (hasMulti ? 1 : 0) + (hasRedirect ? 1 : 0) + (hasOverwrite ? 1 : 0) + (hasInject ? 1 : 0);

        if (roles > 1) {

            return new VifadaError.BadMorph(morphName,

                    "method '" + m.name + m.desc + "' must use only one of "

                            + "@VifadaMulti, @VifadaRedirect, @VifadaOverwrite, @VifadaInject");

        }



        if (hasMulti) {

            return parseVifadaMulti(morphName, desc, m);

        }

        if (hasRedirect) {

            return parseVifadaRedirect(morphName, desc, m);

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

            Result<List<VifadaLocalBinding>, VifadaError> loc = parseParameterLocals(morphName, m);

            if (loc.isErr()) {

                return loc.unwrapErr();

            }

            desc.injects.add(new InjectDescriptor(m, methodSpec, ip, requireTarget, loc.unwrap()));

        }

        return null;

    }



    private static VifadaError parseVifadaMulti(String morphName, MorphDescriptor desc, MethodNode m) {

        AnnotationNode multi = findAnnotation(m.visibleAnnotations, AsmNames.VIFADA_MULTI_DESC);

        if (multi == null) {

            multi = findAnnotation(m.invisibleAnnotations, AsmNames.VIFADA_MULTI_DESC);

        }

        Object rawMethods = annotationValue(multi, "methods");

        List<String> specs = stringArray(rawMethods);

        if (specs.isEmpty()) {

            return new VifadaError.BadMorph(morphName,

                    "@VifadaMulti.methods must not be empty on '" + m.name + m.desc + "'");

        }

        AnnotationNode atAnn = (AnnotationNode) annotationValue(multi, "at");

        if (atAnn == null) {

            return new VifadaError.BadMorph(morphName,

                    "@VifadaMulti on '" + m.name + m.desc + "' must specify at=@VifadaAt(...)");

        }

        InjectionPoint ip = enumValue(atAnn, "value", InjectionPoint.class);

        if (ip == null) {

            return new VifadaError.BadMorph(morphName,

                    "@VifadaAt.value is required for @VifadaMulti on '" + m.name + "'");

        }

        boolean requireTargets = boolOr(annotationValue(multi, "requireTargets"), true);

        Result<List<VifadaLocalBinding>, VifadaError> loc = parseParameterLocals(morphName, m);

        if (loc.isErr()) {

            return loc.unwrapErr();

        }

        List<VifadaLocalBinding> bindings = loc.unwrap();

        for (String spec : specs) {

            if (spec == null || spec.isBlank()) {

                return new VifadaError.BadMorph(morphName, "@VifadaMulti.methods contains blank entry");

            }

            desc.injects.add(new InjectDescriptor(m, spec.trim(), ip, requireTargets, bindings));

        }

        return null;

    }



    private static VifadaError parseVifadaRedirect(String morphName, MorphDescriptor desc, MethodNode m) {

        AnnotationNode red = findAnnotation(m.visibleAnnotations, AsmNames.VIFADA_REDIRECT_DESC);

        if (red == null) {

            red = findAnnotation(m.invisibleAnnotations, AsmNames.VIFADA_REDIRECT_DESC);

        }

        String container = (String) annotationValue(red, "method");

        if (container == null || container.isBlank()) {

            return new VifadaError.BadMorph(morphName,

                    "@VifadaRedirect.method must be set on '" + m.name + m.desc + "'");

        }

        String owner = (String) annotationValue(red, "invokeOwner");

        String invName = (String) annotationValue(red, "invokeName");

        String invDesc = (String) annotationValue(red, "invokeDescriptor");

        if (owner == null || owner.isBlank() || invName == null || invName.isBlank()

                || invDesc == null || invDesc.isBlank()) {

            return new VifadaError.BadMorph(morphName,

                    "@VifadaRedirect invokeOwner/invokeName/invokeDescriptor required on '" + m.name + "'");

        }

        Integer ordBox = (Integer) annotationValue(red, "ordinal");

        int ordinal = ordBox == null ? 0 : ordBox;

        boolean requireTarget = boolOr(annotationValue(red, "requireTarget"), true);

        desc.redirects.add(new RedirectDescriptor(

                m, container.trim(), owner.replace('.', '/'), invName, invDesc, ordinal, requireTarget));

        return null;

    }



    private static Result<List<VifadaLocalBinding>, VifadaError> parseParameterLocals(

            String morphName, MethodNode m) {

        Type[] args = Type.getArgumentTypes(m.desc);

        if (args.length == 0) {

            return Result.ok(List.of());

        }

        if (!args[args.length - 1].equals(AsmNames.CALLBACK_INFO_TYPE)) {

            return Result.ok(List.of());

        }

        int paramCount = args.length - 1;

        List<VifadaLocalBinding> out = new ArrayList<>();

        for (int pi = 0; pi < paramCount; pi++) {

            AnnotationNode locAnn = findParamAnnotation(m, pi, AsmNames.VIFADA_LOCAL_DESC);

            if (locAnn == null) {

                continue;

            }

            Integer ordBox = (Integer) annotationValue(locAnn, "ordinal");

            int ordinal = ordBox == null ? 0 : ordBox;

            String descFilter = (String) annotationValue(locAnn, "descriptor");

            if (descFilter == null) {

                descFilter = "";

            }

            out.add(new VifadaLocalBinding(pi, ordinal, descFilter));

        }

        return Result.ok(out);

    }



    private static AnnotationNode findParamAnnotation(MethodNode m, int index, String annDesc) {

        AnnotationNode a = findInParamArray(m.visibleParameterAnnotations, index, annDesc);

        if (a != null) {

            return a;

        }

        return findInParamArray(m.invisibleParameterAnnotations, index, annDesc);

    }



    /** ASM использует массив списков аннотаций по параметрам. */

    private static AnnotationNode findInParamArray(
            List<AnnotationNode>[] arrays, int index, String annDesc) {

        if (arrays == null || index < 0 || index >= arrays.length) {

            return null;

        }

        List<AnnotationNode> lst = arrays[index];

        if (lst == null) {

            return null;

        }

        return findAnnotation(lst, annDesc);

    }



    private static boolean annotationPresent(MethodNode m, String annDesc) {

        return findAnnotation(m.visibleAnnotations, annDesc) != null

                || findAnnotation(m.invisibleAnnotations, annDesc) != null;

    }



    private static List<String> stringArray(Object rawMethods) {

        List<String> specs = new ArrayList<>();

        if (rawMethods instanceof List<?> lst) {

            for (Object o : lst) {

                if (o instanceof String s) {

                    specs.add(s);

                }

            }

        } else if (rawMethods instanceof String[] arr) {

            for (String s : arr) {

                specs.add(s);

            }

        }

        return specs;

    }



    // ===================================================================

    //                        ANNOTATION HELPERS

    // ===================================================================



    static AnnotationNode findAnnotation(List<AnnotationNode> xs, String desc) {

        if (xs == null) {

            return null;

        }

        for (AnnotationNode a : xs) {

            if (desc.equals(a.desc)) {

                return a;

            }

        }

        return null;

    }



    static Object annotationValue(AnnotationNode a, String name) {

        if (a == null || a.values == null) {

            return null;

        }

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

        if (!(v instanceof String[] arr) || arr.length < 2) {

            return null;

        }

        String valueName = arr[1];

        for (E e : enumType.getEnumConstants()) {

            if (e.name().equals(valueName)) {

                return e;

            }

        }

        return null;

    }



    private static boolean boolOr(Object v, boolean dflt) {

        return v instanceof Boolean b ? b : dflt;

    }

}


