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
 * Полностью заменяет тело одноимённого метода целевого класса.
 *
 * <p>Сигнатура метода морфа должна точно совпадать с сигнатурой целевого
 * (имя + дескриптор). По умолчанию имя и дескриптор для подмены берутся из
 * самого метода морфа; при необходимости их можно уточнить через
 * {@link #method()}.
 */
@ApiStatus.Stable
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface VifadaOverwrite {

    /**
     * Опциональное имя+дескриптор целевого метода в формате
     * {@code name(args)ret}. Пустая строка — использовать сигнатуру
     * самого метода морфа.
     */
    String method() default "";

    /**
     * Если {@code true} и целевой метод не найден — молча пропустить
     * перезапись вместо ошибки. По умолчанию {@code false}.
     */
    boolean silentMissing() default false;
}
