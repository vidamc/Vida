/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import dev.vida.core.ApiStatus;

/**
 * Морф из набора кандидатов, который не был применён к текущему классу.
 *
 * @param morphInternal внутреннее имя класса морфа
 * @param reason        причина (несовпадение {@code target}, превью-API и т.д.)
 */
@ApiStatus.Stable
public record MorphSkip(String morphInternal, String reason) {}
