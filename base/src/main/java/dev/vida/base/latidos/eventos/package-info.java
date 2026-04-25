/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Готовые события Vida: старт/стоп рантайма, тикинг сервера и т.п.
 *
 * <p>Эти типы — тонкие value-классы, которые шина {@link dev.vida.base.latidos.LatidoBus}
 * умеет доставлять. Все они публикуются ядром Vida; моды лишь подписываются
 * на них через статическое поле {@code TIPO}.
 */
@ApiStatus.Stable
package dev.vida.base.latidos.eventos;

import dev.vida.core.ApiStatus;
