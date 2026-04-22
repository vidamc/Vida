/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Метки стабильности публичного API Vida.
 *
 * <p>Каждый публичный тип в модулях {@code vida-*} должен быть помечен одной из
 * этих аннотаций — иначе он считается внутренним и может быть изменён без
 * предупреждения.
 */
public final class ApiStatus {

    private ApiStatus() {}

    /**
     * Стабильный API: совместим в рамках одной MAJOR-версии по SemVer.
     * Нарушение — breaking change, требует MAJOR-бампа.
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
    public @interface Stable {}

    /**
     * Preview-API: может измениться даже в MINOR. Требует включения через
     * {@code [preview] features = [...]} в {@code vida.toml} модератора.
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
    public @interface Preview {
        /** Короткий идентификатор фичи, соответствующий ключу в {@code [preview].features}. */
        String value();
    }

    /**
     * Горячий путь: метод вызывается на каждом тике/кадре или чаще. Реализация
     * обязана избегать аллокаций и рефлексии; статический анализатор
     * {@code vida-gradle-plugin} проверяет это для модов.
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface HotPath {}

    /**
     * Внутренний API: доступен по технической необходимости, но не покрыт
     * гарантиями совместимости. Использовать только если явно задокументировано.
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
    public @interface Internal {
        /** Необязательная причина: почему пользователь API не должен это трогать. */
        String value() default "";
    }

    /**
     * Помечает устаревший API и подсказывает план миграции.
     */
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})
    public @interface Deprecated {
        /** Версия, в которой API будет удалён ({@code MAJOR.MINOR} Vida). */
        String removalIn() default "";

        /** Рекомендованная замена. */
        String replacement() default "";
    }
}
