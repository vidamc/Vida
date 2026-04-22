/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada.internal;

import dev.vida.core.ApiStatus;
import dev.vida.vifada.InjectionPoint;
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
 */
@ApiStatus.Internal
public record InjectDescriptor(MethodNode method, String targetMethod,
                               InjectionPoint at, boolean requireTarget) {

    public InjectDescriptor {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(targetMethod, "targetMethod");
        Objects.requireNonNull(at, "at");
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
