/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base;

import dev.vida.base.ajustes.AjustesTipados;
import dev.vida.base.catalogo.CatalogoManejador;
import dev.vida.base.latidos.LatidoBus;
import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import dev.vida.core.Version;
import java.nio.file.Path;

/**
 * «Паспорт мода» — единственный объект, который загрузчик передаёт в
 * {@link VidaMod#iniciar}.
 *
 * <p>Через него мод получает всё, что ему может понадобиться:
 * <ul>
 *   <li>свою метадату ({@link #metadata});</li>
 *   <li>шину событий ({@link #latidos});</li>
 *   <li>реестры ({@link #catalogos});</li>
 *   <li>настройки ({@link #ajustes});</li>
 *   <li>стандартный логгер ({@link #log});</li>
 *   <li>директорию для приватных данных мода ({@link #directorioDatos}).</li>
 * </ul>
 *
 * <p>Контекст <b>не</b> даёт доступ к другим модам напрямую — это
 * намеренное решение: мод должен взаимодействовать с другими модами
 * только через публичный API (события и реестры).
 */
@ApiStatus.Preview("base")
public interface ModContext {

    /** Идентификатор мода ({@code vida.mod.json:id}). */
    String id();

    /** Версия мода. */
    Version version();

    /** Полная метадата мода (срез из манифеста). */
    ModMetadata metadata();

    /** Шина событий. Одна на рантайм; разделяется между всеми модами. */
    LatidoBus latidos();

    /** Реестры. Один {@link CatalogoManejador} на рантайм. */
    CatalogoManejador catalogos();

    /** Типизированные настройки, уже scoped под этот мод. */
    AjustesTipados ajustes();

    /** Стандартный логгер мода ({@code vida.mod.<id>}). */
    Log log();

    /**
     * Директория для приватных данных мода. Гарантированно существует
     * (создаётся загрузчиком при необходимости), принадлежит только этому
     * моду. Путь стабилен между запусками.
     */
    Path directorioDatos();
}
