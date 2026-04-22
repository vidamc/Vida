/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery.internal;

import dev.vida.core.ApiStatus;
import dev.vida.manifest.ModManifest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Извлечение списка вложенных JAR из манифеста.
 *
 * <p>Vida-конвенция: в {@code vida.mod.json} раздел {@code "custom"} может
 * содержать поле {@code "jars"} — массив строк с путями внутри JAR, по которым
 * находятся вложенные архивы. Формальное расширение схемы отложено до
 * schema v2; на v1 этот ключ читается как часть общего blob-а {@code custom}.
 *
 * <pre>{@code
 * {
 *   "schema": 1,
 *   "id": "example",
 *   "version": "1.0.0",
 *   "name": "Example",
 *   "custom": {
 *     "jars": ["META-INF/jars/inner.jar"]
 *   }
 * }
 * }</pre>
 */
@ApiStatus.Internal
public final class NestedJars {

    public static final String CUSTOM_KEY = "jars";

    private NestedJars() {}

    /** Возвращает список относительных путей; пустой список, если ключ не задан. */
    public static List<String> from(ModManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        Object raw = manifest.custom().get(CUSTOM_KEY);
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<String> out = new ArrayList<>(list.size());
        for (Object e : list) {
            if (e instanceof String s && !s.isBlank()) {
                out.add(s);
            }
        }
        return List.copyOf(out);
    }
}
