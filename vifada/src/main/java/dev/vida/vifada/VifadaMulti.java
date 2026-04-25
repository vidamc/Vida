/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import dev.vida.core.ApiStatus;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Один инъектор применяется к нескольким target-методам одной и той же точкой {@link VifadaAt}.
 *
 * <p>Все методы из {@link #methods()} должны быть совместимы с параметрами метода морфа
 * (последний параметр — {@link CallbackInfo}), как для {@link VifadaInject}.
 */
@ApiStatus.Stable
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface VifadaMulti {

    /**
     * Набор target-методов в формате {@code name(desc)ret}.
     */
    String[] methods();

    /**
     * Единая точка инъекции для всех target-методов.
     */
    VifadaAt at();

    /**
     * Если {@code true}, отсутствие части target-методов не считается ошибкой.
     */
    boolean requireTargets() default true;
}
