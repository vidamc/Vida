/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada.internal;

import dev.vida.core.ApiStatus;
import dev.vida.vifada.InjectionPoint;
import java.util.List;
import java.util.Objects;
import org.objectweb.asm.tree.MethodNode;

/**
 * Один {@link dev.vida.vifada.VifadaInject}-метод морфа, готовый к аппликации.
 *
 * @param method        MethodNode морфа (с телом и аннотациями)
 * @param targetMethod  имя+дескриптор целевого метода в формате
 *                      {@code name(args)ret}
 * @param at            точка инъекции
 * @param requireTarget требовать ли наличия целевого метода (иначе — silent)
 * @param localBindings привязки параметров к LVT через {@link dev.vida.vifada.VifadaLocal}
 */
@ApiStatus.Internal
public record InjectDescriptor(MethodNode method, String targetMethod,
                               InjectionPoint at, boolean requireTarget,
                               List<VifadaLocalBinding> localBindings) {

    public InjectDescriptor(MethodNode method, String targetMethod,
                            InjectionPoint at, boolean requireTarget) {
        this(method, targetMethod, at, requireTarget, List.of());
    }

    public InjectDescriptor {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(targetMethod, "targetMethod");
        Objects.requireNonNull(at, "at");
        localBindings = localBindings == null ? List.of() : List.copyOf(localBindings);
    }

    public String targetName() {
        int paren = targetMethod.indexOf('(');
        return paren < 0 ? targetMethod : targetMethod.substring(0, paren);
    }

    public String targetDescriptor() {
        int paren = targetMethod.indexOf('(');
        return paren < 0 ? "" : targetMethod.substring(paren);
    }
}
