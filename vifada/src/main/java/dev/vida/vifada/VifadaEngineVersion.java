/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import dev.vida.core.ApiStatus;

/**
 * Версия движка трансформации Vifada — участвует в ключе дискового кеша байткода.
 *
 * <p>Увеличивайте значение при изменении семантики {@link Transformer}/{@code MorphApplier},
 * чтобы инвалидировать старые записи кеша.
 */
@ApiStatus.Stable
public final class VifadaEngineVersion {

    private VifadaEngineVersion() {}

    /** Текущая линия движка (не SemVer артефакта, а логическая версия пайплайна). */
    public static final String VERSION = "2";
}
