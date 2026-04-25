/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Публичный API Vida для предметов Minecraft 1.21.1.
 *
 * <p>Модель:
 * <ul>
 *   <li>{@link dev.vida.objeto.Objeto} — сам предмет (id + свойства);</li>
 *   <li>{@link dev.vida.objeto.PropiedadesObjeto} — иммутабельный набор
 *       свойств через fluent-билдер;</li>
 *   <li>{@link dev.vida.objeto.componentes} — data-компоненты 1.21.1 как
 *       типизированные записи;</li>
 *   <li>{@link dev.vida.objeto.Herramienta} — категоризация инструментов
 *       (pickaxe/axe/…) и нива {@link dev.vida.objeto.Material};</li>
 *   <li>{@link dev.vida.objeto.registro.RegistroObjetos} — регистрация в
 *       общем {@link dev.vida.base.catalogo.CatalogoManejador}.</li>
 * </ul>
 *
 * <p>Модуль совместим и с клиентом, и с сервером; vanilla-мост живёт в
 * будущем {@code vida-mundo}.
 */
@ApiStatus.Stable
package dev.vida.objeto;

import dev.vida.core.ApiStatus;
