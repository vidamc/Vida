/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import dev.vida.core.ApiStatus;

/**
 * Resolves Mojmap-style method selectors to the obfuscated name and descriptor
 * actually present in the runtime class file.
 *
 * <p>Used when {@link Transformer} is given a {@linkplain Transformer#transform(
 * byte[], java.util.Collection, String, MorphMethodResolution) morph target key}
 * that matches Mojmap class names while {@code ClassNode#name} is obfuscated.
 */
@ApiStatus.Stable
@FunctionalInterface
public interface MorphMethodResolution {

    /**
     * @param obfClassInternal    {@code ClassNode#name} of the class being transformed
     * @param mojmapMethodName    method name as written in {@code @VifadaInject} /
     *                            {@code @VifadaOverwrite} (Mojmap)
     * @param mojmapDescriptor    JVM method descriptor with Mojmap internal class names
     * @return {@code { obfName, obfDescriptor }} or {@code null} if unknown
     */
    String[] resolveObfMethod(String obfClassInternal, String mojmapMethodName, String mojmapDescriptor);
}
