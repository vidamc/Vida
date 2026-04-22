/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base;

import dev.vida.core.ApiStatus;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Помечает класс, реализующий {@link VidaMod}, как «эталонную точку входа»
 * мода.
 *
 * <p>Загрузчик Vida умеет искать entrypoint двумя путями:
 * <ol>
 *   <li>через поле {@code entrypoint} в {@code vida.mod.json}; это
 *       предпочтительный путь — он дешевле при старте;</li>
 *   <li>через сканирование классов мода в поисках {@link VidaEntrypoint};
 *       используется, если в манифесте entrypoint не указан.</li>
 * </ol>
 *
 * <p>Если в одном моде несколько {@link VidaEntrypoint}, загрузчик выбирает
 * entrypoint с наибольшим {@link #prioridad()}; при равных приоритетах —
 * по {@link String#compareTo лексикографическому} порядку FQN класса.
 */
@ApiStatus.Preview("base")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VidaEntrypoint {

    /**
     * Дополнительная «метка» entrypoint. Позволяет моду иметь несколько
     * entrypoint'ов для разных окружений. Пустая строка — общая точка.
     */
    String etiqueta() default "";

    /** Приоритет выбора, когда в моде несколько entrypoint'ов. */
    int prioridad() default 0;
}
