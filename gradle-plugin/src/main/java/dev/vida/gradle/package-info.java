/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Gradle-плагин {@code dev.vida.mod}.
 *
 * <h2>Зачем</h2>
 *
 * <p>Мод-разработчик подключает в свой проект:
 *
 * <pre>{@code
 * plugins {
 *     id("dev.vida.mod") version "0.1.0"
 * }
 *
 * vida {
 *     mod {
 *         id.set("miaventura")
 *         displayName.set("Mi Aventura")
 *         description.set("...")
 *         authors.add("Ana")
 *         entrypoint.set("com.ejemplo.MiAventura")
 *     }
 *     minecraft {
 *         version.set("1.21.1")
 *         mappings {
 *             proguard.set(file("mappings/mojang_1_21_1.txt"))
 *         }
 *     }
 *     run {
 *         mainClass.set("net.minecraft.client.Main")
 *         args.addAll("--accessToken", "dev")
 *     }
 * }
 * }</pre>
 *
 * <h2>Задачи</h2>
 *
 * <ul>
 *   <li>{@code vidaGenerateManifest} — генерирует {@code vida.mod.json}
 *       из DSL в {@code build/generated/vida/resources};</li>
 *   <li>{@code vidaValidateManifest} — прогоняет его через
 *       {@link dev.vida.manifest.ManifestParser};</li>
 *   <li>{@code vidaRemapJar} — применяет {@link dev.vida.cartografia.asm.CartografiaRemapper}
 *       к {@code .jar};</li>
 *   <li>{@code vidaRun} — запускает Vida с указанной игрой через
 *       {@code JavaExec}.</li>
 * </ul>
 *
 * <h2>Интеграция с сборкой</h2>
 *
 * Плагин автоматически применяет {@code java-library}, подкладывает
 * сгенерированный {@code vida.mod.json} в ресурсы {@code main}, ставит
 * задачи {@code jar} в зависимость от генерации и валидации манифеста,
 * регистрирует группу задач {@code vida} для удобства поиска в UI IDE.
 */
@ApiStatus.Stable
package dev.vida.gradle;

import dev.vida.core.ApiStatus;
