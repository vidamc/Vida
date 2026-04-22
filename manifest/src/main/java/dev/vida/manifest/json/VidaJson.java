/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.manifest.json;

import dev.vida.core.ApiStatus;
import java.io.Reader;

/**
 * Точка входа в Vida-JSON: создание {@link JsonReader}.
 *
 * <p>Класс-фасад. Здесь же при необходимости появятся конвертеры в
 * generic-структуру ({@code Object}-дерево) — но на hot path они не нужны и
 * намеренно отсутствуют в начальной версии.
 */
@ApiStatus.Stable
public final class VidaJson {

    private VidaJson() {}

    public static JsonReader reader(String json) {
        return JsonReader.of(json);
    }

    public static JsonReader reader(Reader source) {
        return JsonReader.of(source);
    }
}
