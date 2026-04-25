/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.internal;

import dev.vida.core.ApiStatus;
import java.nio.file.Path;

/** Стабильные пути каталогов кеша на разных ОС. */
@ApiStatus.Internal
public final class VidaCacheLayout {

    private VidaCacheLayout() {}

    /**
     * Корень кеша по умолчанию: {@code %LOCALAPPDATA%/Vida/cache} на Windows,
     * иначе {@code ~/.vida/cache}.
     */
    public static Path defaultRoot() {
        String local = System.getenv("LOCALAPPDATA");
        if (local != null && !local.isBlank()) {
            return Path.of(local, "Vida", "cache");
        }
        return Path.of(System.getProperty("user.home", "."), ".vida", "cache");
    }

    /** Подкаталог для кеша результатов {@code Transformer}. */
    public static Path transformBytecodeDir(Path cacheRoot) {
        return cacheRoot.resolve("transform");
    }

    /**
     * Вложенные JAR модов ({@link dev.vida.discovery.ModSource.Embedded}) материализуются
     * на диск под этим каталогом для {@link java.net.URLClassLoader}.
     */
    public static Path embeddedModsDir(Path cacheRoot) {
        return cacheRoot.resolve("embedded-mods");
    }
}
