/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada.internal;

import dev.vida.core.ApiStatus;
import java.util.Objects;
import org.objectweb.asm.tree.MethodNode;

/**
 * Один {@link dev.vida.vifada.VifadaOverwrite}-метод: что чем заменяется.
 *
 * @param method         MethodNode морфа, тело которого будет скопировано
 * @param targetMethod   имя+дескриптор целевого метода ({@code name(args)ret})
 * @param silentMissing  если {@code true}, отсутствие цели не ошибка
 */
@ApiStatus.Internal
public record OverwriteDescriptor(MethodNode method, String targetMethod, boolean silentMissing) {

    public OverwriteDescriptor {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(targetMethod, "targetMethod");
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
