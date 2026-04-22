/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.core.ApiStatus;
import java.net.URL;

/**
 * ClassLoader игровой стороны (от испанского <i>juego</i> — игра).
 *
 * <p>Сюда попадают classpath игры (Minecraft + её зависимости). Он
 * является корневым для всех {@link ModLoader}'ов: мод видит игровые
 * классы, но не видит классы соседних модов (если только они не
 * перечислены в его зависимостях и явно объединены при сборке).
 */
@ApiStatus.Preview("loader")
public final class JuegoLoader extends TransformingClassLoader {

    static { ClassLoader.registerAsParallelCapable(); }

    public JuegoLoader(URL[] gameUrls, ClassLoader parent, VidaClassTransformer transformer) {
        super("vida-juego", gameUrls, parent, transformer);
    }
}
