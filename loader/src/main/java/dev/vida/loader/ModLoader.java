/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.core.ApiStatus;
import java.net.URL;
import java.util.Objects;

/**
 * ClassLoader одного мода. Родитель — {@link JuegoLoader}.
 *
 * <p>Каждому разрешённому моду в финальной пачке создаётся свой
 * {@link ModLoader}: так два мода не видят друг друга напрямую (если им
 * не разрешено через графу зависимостей), а конфликт имён классов
 * между разными модами даёт отдельные {@link Class}-объекты, а не
 * рантайм-падение при первом loadClass.
 */
@ApiStatus.Preview("loader")
public final class ModLoader extends TransformingClassLoader {

    static { ClassLoader.registerAsParallelCapable(); }

    private final String modId;

    public ModLoader(String modId, URL[] modUrls, JuegoLoader parent,
                     VidaClassTransformer transformer) {
        super("vida-mod:" + Objects.requireNonNull(modId, "modId"),
                modUrls, parent, transformer);
        this.modId = modId;
    }

    public String modId() { return modId; }
}
