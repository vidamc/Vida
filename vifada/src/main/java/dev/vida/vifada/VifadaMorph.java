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
 * Помечает класс как морф, применяемый к указанному целевому классу.
 *
 * <p>Атрибут {@link #target()} задаётся как полностью квалифицированное имя
 * целевого класса с точками в качестве разделителей — например,
 * {@code "net.minecraft.client.Minecraft"}.
 *
 * <p>Морф всегда должен быть {@code abstract} (или, строго говоря, никогда
 * не инстанцироваться мод-разработчиком напрямую) — это декларативное
 * описание, а не рабочий класс. Его методы после разбора копируются в
 * целевой класс с переадресацией ссылок.
 */
@ApiStatus.Preview("vifada")
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface VifadaMorph {

    /** FQN целевого класса с точками как разделителями. */
    String target();

    /**
     * Приоритет применения морфа относительно других морфов того же
     * целевого класса. Меньшее число — раньше. Дефолт {@code 1000}.
     * Порядок важен, например, для HEAD-инъекций: более ранний морф
     * выполняется первым.
     */
    int priority() default 1000;
}
