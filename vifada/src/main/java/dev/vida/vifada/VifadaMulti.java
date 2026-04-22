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
 * Preview-расширение Vifada: один инъектор можно применить к нескольким target-методам.
 *
 * <p>Аннотация пока не обрабатывается transformer'ом и служит контрактом раннего
 * API для следующей линии развития {@code vifada-next}.
 */
@ApiStatus.Preview("vifada-next")
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
