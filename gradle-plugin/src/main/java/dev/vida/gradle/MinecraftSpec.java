/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle;

import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

/**
 * DSL-блок «Minecraft»: версия игры и маппинги.
 *
 * <pre>{@code
 * vida.minecraft {
 *     version.set("1.21.1")
 *     clientJar.set(file("gamedata/minecraft-1.21.1-client.jar"))
 *     mappings {
 *         proguard.set(file("mappings/mojang_1_21_1.txt"))
 *     }
 * }
 * }</pre>
 */
public abstract class MinecraftSpec {

    private final MappingsSpec mappings;

    @Inject
    public MinecraftSpec(ObjectFactory objects) {
        this.mappings = objects.newInstance(MappingsSpec.class);
        mappings.getObfNamespace().convention("obf");
        mappings.getNamedNamespace().convention("named");
    }

    /** Целевая версия Minecraft, напр. {@code "1.21.1"}. */
    public abstract Property<String> getVersion();

    /** Путь к оригинальному клиентскому jar (для ремаппинга и vidaRun). */
    public abstract RegularFileProperty getClientJar();

    /** Путь к оригинальному серверному jar. */
    public abstract RegularFileProperty getServerJar();

    public MappingsSpec getMappings() {
        return mappings;
    }

    /** Groovy/Kotlin DSL: {@code mappings { ... }}. */
    public void mappings(Action<? super MappingsSpec> action) {
        action.execute(mappings);
    }
}
