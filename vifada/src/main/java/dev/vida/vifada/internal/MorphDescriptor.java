/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada.internal;

import dev.vida.core.ApiStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.objectweb.asm.tree.ClassNode;

/**
 * Распарсенное описание морфа: его собственный класс, целевой класс и
 * списки инъекций/перезаписей/теней.
 */
@ApiStatus.Internal
public final class MorphDescriptor {

    /** internal имя морфа, например {@code com/example/FooMorph}. */
    public final String morphInternal;
    /** internal имя целевого класса, например {@code com/example/Foo}. */
    public final String targetInternal;
    public final int priority;
    public final ClassNode morphNode;

    public final List<InjectDescriptor> injects = new ArrayList<>();
    public final List<OverwriteDescriptor> overwrites = new ArrayList<>();
    public final List<ShadowDescriptor> shadows = new ArrayList<>();

    public MorphDescriptor(String morphInternal, String targetInternal,
                           int priority, ClassNode morphNode) {
        this.morphInternal = Objects.requireNonNull(morphInternal);
        this.targetInternal = Objects.requireNonNull(targetInternal);
        this.morphNode = Objects.requireNonNull(morphNode);
        this.priority = priority;
    }

    public boolean isEmpty() {
        return injects.isEmpty() && overwrites.isEmpty();
    }

    @Override
    public String toString() {
        return "MorphDescriptor[" + morphInternal + " → " + targetInternal
                + ", injects=" + injects.size()
                + ", overwrites=" + overwrites.size()
                + ", shadows=" + shadows.size() + "]";
    }
}
