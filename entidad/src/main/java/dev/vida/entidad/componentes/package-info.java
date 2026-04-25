/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Типизированные data-components сущностей.
 *
 * <p>Entity-components отделены от item-components намеренно: bridge к
 * Minecraft runtime разный, а явное разделение даёт более строгую
 * типизацию для модов и для будущей сериализации.
 */
@ApiStatus.Stable
package dev.vida.entidad.componentes;

import dev.vida.core.ApiStatus;
