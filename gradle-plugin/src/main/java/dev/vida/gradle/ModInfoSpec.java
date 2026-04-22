/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle;

import java.util.Map;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

/**
 * DSL-блок «информация о моде». Полностью соответствует {@code vida.mod.json}.
 *
 * <pre>{@code
 * vida {
 *     mod {
 *         id.set("demo")
 *         version.set("1.0.0")
 *         displayName.set("Demo Mod")
 *         description.set("Beautiful demo")
 *         authors.addAll("Ana", "Bob")
 *         license.set("MIT")
 *         entrypoint.set("com.ejemplo.DemoMod")
 *         puertas.add("config/access.puertas")
 *         dependencies.put("vida", "^0.1")
 *     }
 * }
 * }</pre>
 */
public interface ModInfoSpec {

    /** Идентификатор мода (подмножество {@code Identifier}: {@code [a-z0-9_.-]+}). */
    Property<String> getId();

    /** Версия мода (SemVer 2.0). Если не задана — берётся из {@code project.version}. */
    Property<String> getVersion();

    /** Человекочитаемое имя. По умолчанию — {@code id}. */
    Property<String> getDisplayName();

    /** Описание. */
    Property<String> getDescription();

    /** Лицензия (SPDX id). */
    Property<String> getLicense();

    /** Полное имя класса-entrypoint (должен реализовывать {@code VidaMod}). */
    Property<String> getEntrypoint();

    /** Список авторов. */
    ListProperty<String> getAuthors();

    /** Список {@code .puertas}-файлов (access transformers). */
    ListProperty<String> getPuertas();

    /** Список {@code .escultor}-файлов (manual class transformers). */
    ListProperty<String> getEscultores();

    /** Зависимости: {@code id -> version range}. */
    MapProperty<String, String> getDependencies();

    /** Опциональные зависимости. */
    MapProperty<String, String> getOptionalDependencies();

    /** Конфликтующие моды. */
    ListProperty<String> getIncompatibilities();

    /** Число схемы {@code vida.mod.json}. По умолчанию — {@code 1}. */
    Property<Integer> getSchema();

    // ---- удобные shortcut-методы для Groovy-DSL ----

    default void dependency(String id, String range) {
        getDependencies().put(id, range);
    }

    default void optionalDependency(String id, String range) {
        getOptionalDependencies().put(id, range);
    }

    default void dependencies(Map<String, String> deps) {
        getDependencies().putAll(deps);
    }
}
