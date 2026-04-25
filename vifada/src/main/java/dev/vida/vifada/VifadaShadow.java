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
 * Объявляет поле или метод, которое присутствует в целевом классе.
 *
 * <p>Авторская сторона: объявляете «теневую» копию с тем же именем и типом/
 * сигнатурой — это нужно, чтобы компилятор разрешал обращения к ней
 * внутри морфа. Во время трансформации все ссылки на shadow-члена
 * переадресуются на целевой класс, а сам shadow-член в итоговом классе
 * не появляется.
 */
@ApiStatus.Stable
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface VifadaShadow {

    /**
     * Если {@code true}, отсутствие соответствующего члена в целевом
     * классе — молчаливая ошибка. По умолчанию {@code false}.
     */
    boolean silentMissing() default false;
}
