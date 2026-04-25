/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Типизированные реестры Vida — <i>Catálogo</i>.
 *
 * <h2>Зачем</h2>
 *
 * <p>В modding-е стандартная потребность: «вот у меня есть сортовая
 * коллекция блоков/предметов/энтити/звуков, я хочу её иметь под стабильным
 * ключом, защитить от повторной регистрации и выдать мне численный id для
 * сетевой сериализации». В Vida для этого есть {@link dev.vida.base.catalogo.Catalogo}.
 *
 * <h2>Модель</h2>
 *
 * <ul>
 *   <li>{@link dev.vida.base.catalogo.CatalogoClave CatalogoClave&lt;T&gt;} — типизированный ключ {@code namespace:path};</li>
 *   <li>{@link dev.vida.base.catalogo.Catalogo Catalogo&lt;T&gt;} — read-only представление реестра;</li>
 *   <li>{@link dev.vida.base.catalogo.CatalogoMutable CatalogoMutable&lt;T&gt;} — фаза регистрации, в которой реестр можно пополнять;</li>
 *   <li>{@link dev.vida.base.catalogo.Inscripcion Inscripcion&lt;T&gt;} — handle, возвращаемый регистрацией: держит клавишу и ленивое значение;</li>
 *   <li>{@link dev.vida.base.catalogo.CatalogoManejador CatalogoManejador} — «шкаф каталогов»: один на рантайм, владеет множеством конкретных реестров.</li>
 * </ul>
 */
@ApiStatus.Stable
package dev.vida.base.catalogo;

import dev.vida.core.ApiStatus;
