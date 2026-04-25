/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.internal;

import dev.vida.cartografia.ClassMapping;
import dev.vida.cartografia.MappingTree;
import dev.vida.cartografia.MethodMapping;
import dev.vida.cartografia.Namespace;
import dev.vida.core.ApiStatus;
import dev.vida.vifada.MorphMethodResolution;

/**
 * Resolves Mojmap method selectors using a Cartografía {@link MappingTree}
 * (Mojang client_mappings.txt).
 */
@ApiStatus.Internal
public final class CartografiaMorphMethodResolution implements MorphMethodResolution {

    private final MappingTree tree;

    public CartografiaMorphMethodResolution(MappingTree tree) {
        this.tree = tree;
    }

    @Override
    public String[] resolveObfMethod(String obfClassInternal, String mojmapMethodName, String mojmapDescriptor) {
        ClassMapping cm = tree.classByName(Namespace.OBF, obfClassInternal);
        if (cm == null) {
            return null;
        }
        String obfDescriptor;
        try {
            obfDescriptor = tree.remapDescriptor(Namespace.MOJMAP, Namespace.OBF, mojmapDescriptor);
        } catch (RuntimeException ex) {
            obfDescriptor = mojmapDescriptor;
        }
        for (MethodMapping mm : cm.methods()) {
            if (!mojmapMethodName.equals(mm.name(Namespace.MOJMAP))) {
                continue;
            }
            if (!obfDescriptor.equals(mm.sourceDescriptor())) {
                continue;
            }
            return new String[] {mm.sourceName(), mm.sourceDescriptor()};
        }
        return null;
    }
}
