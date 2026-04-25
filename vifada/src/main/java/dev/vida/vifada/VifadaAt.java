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
 * Описание точки инъекции: что именно и где в целевом методе.
 *
 * <p>Применяется как значение атрибута {@link VifadaInject#at()}.
 *
 * <p>В MVP значимо только поле {@link #value()}; остальные поля
 * зарезервированы для расширенных типов точек ({@link InjectionPoint#INVOKE},
 * {@link InjectionPoint#FIELD} и пр.).
 */
@ApiStatus.Stable
@Retention(RetentionPolicy.CLASS)
@Target({})
public @interface VifadaAt {

    /** Тип точки инъекции. */
    InjectionPoint value();

    /**
     * Дополнительный селектор для точек вида INVOKE/FIELD — например,
     * сигнатура вызываемого метода. В MVP игнорируется.
     */
    String target() default "";

    /**
     * Порядковый номер совпадения, если точек несколько (1 — первая,
     * 0 — все). В MVP игнорируется.
     */
    int ordinal() default 0;
}
