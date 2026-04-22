/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import dev.vida.core.ApiStatus;

/**
 * Типы зависимостей, понимаемые резолвером.
 */
@ApiStatus.Stable
public enum DependencyKind {

    /**
     * Жёсткая зависимость. Если подходящая версия не найдена, резолв
     * проваливается с {@link ResolverError.Missing} или
     * {@link ResolverError.VersionConflict}.
     */
    REQUIRED,

    /**
     * Мягкая зависимость. Если подходящая версия есть — она выбирается
     * и может учитываться другими модами; если нет — резолвер молча
     * идёт дальше и помечает id в
     * {@link Resolution#optionalMissing()}. Оптинальные требования
     * никогда не триггерят backtrack вышестоящих решений.
     */
    OPTIONAL,

    /**
     * Явная несовместимость: мод заявляет, что не работает вместе с
     * другим модом в указанном диапазоне. Если в резолюции оба оказались
     * выбранными, возвращается {@link ResolverError.Incompatibility}.
     */
    INCOMPATIBLE
}
