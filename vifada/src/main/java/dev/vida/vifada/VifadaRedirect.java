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
 * Перенаправляет конкретный вызов метода в теле целевого метода на метод морфа.
 *
 * <p>Метод морфа должен иметь ту же сигнатуру (включая статичность), что и
 * вызываемый метод: для {@code INVOKESTATIC} — статический метод с теми же
 * аргументами и возвратом; для {@code INVOKEVIRTUAL}/{@code INVOKESPECIAL} —
 * статический метод, чей первый параметр — {@code this} получателя, далее
 * аргументы оригинального вызова.
 */
@ApiStatus.Stable
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface VifadaRedirect {

    /**
     * Метод контейнера в целевом классе: {@code name(args)ret}, к примеру
     * {@code sum(II)I}.
     */
    String method();

    /**
     * Внутреннее имя владельца вызова ({@code java/lang/Math}).
     */
    String invokeOwner();

    /** Имя вызываемого метода. */
    String invokeName();

    /** JVM-дескриптор вызываемого метода. */
    String invokeDescriptor();

    /**
     * Какой по порядку (0-based) подходящий вызов заменить, если их несколько.
     */
    int ordinal() default 0;

    /**
     * Если {@code false}, отсутствие контейнера или вызова молча пропускается.
     */
    boolean requireTarget() default true;
}
