/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import dev.vida.core.ApiStatus;

/**
 * Стратегия выбора кандидатов в рамках одного id.
 *
 * <p>Влияет только на порядок перебора: резолвер всегда пробует всех
 * кандидатов, пока не найдёт решение или не исчерпает их.
 */
@ApiStatus.Stable
public enum ResolverStrategy {

    /**
     * Сначала самая новая SemVer-версия, затем по убыванию. Pre-release
     * версии идут после релизных того же {@code MAJOR.MINOR.PATCH},
     * потому что у SemVer pre &lt; release.
     */
    NEWEST,

    /**
     * Сначала только релизные версии (по убыванию), затем pre-release
     * (по убыванию). Снижает шанс втянуть alpha/beta без явного запроса.
     */
    STABLE_FIRST,

    /**
     * Сначала самая старая подходящая версия, затем по возрастанию.
     * Полезно для воспроизводимых тестов и стабилизационных сборок.
     */
    OLDEST
}
