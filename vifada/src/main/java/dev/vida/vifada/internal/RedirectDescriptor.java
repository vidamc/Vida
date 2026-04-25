/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada.internal;

import dev.vida.core.ApiStatus;
import java.util.Objects;
import org.objectweb.asm.tree.MethodNode;

/**
 * Замена конкретного вызова метода ({@code MethodInsnNode}) на вызов хелпера морфа.
 */
@ApiStatus.Internal
public record RedirectDescriptor(
        MethodNode morphMethod,
        String containerMethodSpec,
        String invokeOwner,
        String invokeName,
        String invokeDescriptor,
        int ordinal,
        boolean requireTarget) {

    public RedirectDescriptor {
        Objects.requireNonNull(morphMethod, "morphMethod");
        Objects.requireNonNull(containerMethodSpec, "containerMethodSpec");
        Objects.requireNonNull(invokeOwner, "invokeOwner");
        Objects.requireNonNull(invokeName, "invokeName");
        Objects.requireNonNull(invokeDescriptor, "invokeDescriptor");
        if (ordinal < 0) {
            throw new IllegalArgumentException("ordinal must be >= 0");
        }
    }

    public String containerName() {
        int paren = containerMethodSpec.indexOf('(');
        return paren < 0 ? containerMethodSpec : containerMethodSpec.substring(0, paren);
    }

    public String containerDescriptor() {
        int paren = containerMethodSpec.indexOf('(');
        return paren < 0 ? "" : containerMethodSpec.substring(paren);
    }
}
