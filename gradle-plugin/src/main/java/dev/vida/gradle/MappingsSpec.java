/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/**
 * Блок настроек маппингов.
 *
 * <p>Vida умеет читать proguard-mappings (Mojang-формат) и собственный
 * компактный {@code .ctg}. Один из двух путей обязателен, если проекту
 * нужно запускать {@code vidaRemapJar}.
 *
 * <pre>{@code
 * vida.minecraft {
 *     mappings {
 *         // обычный Mojang-формат
 *         proguard.set(file("mappings/mojang_1_21_1.txt"))
 *         obfNamespace.set("obf")
 *         namedNamespace.set("named")
 *     }
 * }
 * }</pre>
 */
public interface MappingsSpec {

    /** Proguard-файл (Mojang-формат). Взаимоисключительно с {@link #getCtg}. */
    RegularFileProperty getProguard();

    /** Собственный компактный формат Vida {@code .ctg}. */
    RegularFileProperty getCtg();

    /** Имя namespace обфусцированных имён (source); по умолчанию {@code "obf"}. */
    Property<String> getObfNamespace();

    /** Имя namespace читаемых имён (target); по умолчанию {@code "named"}. */
    Property<String> getNamedNamespace();
}
