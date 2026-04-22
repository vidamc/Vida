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
 * Preview-расширение Vifada: декларация доступа к локальной переменной target-метода.
 *
 * <p>Предполагается для использования на параметрах метода-инъектора.
 * Реализация планируется в {@code vifada-next}; пока аннотация доступна как
 * compile-time контракт.
 */
@ApiStatus.Preview("vifada-next")
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.PARAMETER)
public @interface VifadaLocal {

    /**
     * Номер совпадения локальной переменной в LVT.
     */
    int ordinal() default 0;

    /**
     * Ограничение по JVM-дескриптору, если в точке есть переменные одинакового типа.
     */
    String descriptor() default "";

    /**
     * Требуется ли mutable-доступ к переменной.
     */
    boolean mutable() default false;
}
