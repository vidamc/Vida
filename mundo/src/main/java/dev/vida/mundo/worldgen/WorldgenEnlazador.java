/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo.worldgen;

import dev.vida.core.ApiStatus;
import java.nio.file.Path;

/**
 * Контракт платформенного моста: Fuente уже разбирает JSON под {@code worldgen/}
 * и проверяет ссылки на loot и блоки модуля; этот интерфейс — место, где рантайм
 * игры должен связать те же ресурсы datapack с реальной генерацией мира
 * (configured/placed features, structures и т.д.).
 *
 * <p>Реализация живёт в загрузчике / bridge к конкретной версии Minecraft, не в
 * {@code vida-mundo} как чистой модели.
 */
@ApiStatus.Preview("worldgenEnlazador")
public interface WorldgenEnlazador {

    /**
     * Регистрация worldgen-ресурсов мода из корня ресурсов (например JAR или
     * распакованного datapack).
     *
     * @param modId идентификатор мода (namespace datapack)
     * @param raizRecursos корень, где лежит {@code data/} (часто {@code src/main/resources})
     */
    void registrarDesdeRecursos(String modId, Path raizRecursos);
}
