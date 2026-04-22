/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Интеграция Vida со стандартной файловой раскладкой лаунчеров Minecraft.
 *
 * <p>Пакет отвечает за то, чтобы ванильный Mojang Launcher, MultiMC, Prism и
 * ATLauncher увидели Vida как обычный профиль Minecraft:
 *
 * <ul>
 *   <li>{@link dev.vida.installer.mc.McLayout} — канонические пути
 *       ({@code libraries/}, {@code versions/}, {@code launcher_profiles.json}, ...);</li>
 *   <li>{@link dev.vida.installer.mc.VersionJson} — модель и writer
 *       стандартного Minecraft version-JSON (с {@code inheritsFrom},
 *       {@code libraries}, {@code arguments.jvm});</li>
 *   <li>{@link dev.vida.installer.mc.LauncherProfilesPatcher} —
 *       атомарный патчер {@code launcher_profiles.json}: добавляет или
 *       обновляет запись Vida, полностью сохраняя остальные профили;</li>
 *   <li>{@link dev.vida.installer.mc.JsonTree} — маленькая tree-model
 *       поверх {@code :manifest}-ного {@code JsonReader}, используемая для
 *       патчей, которым нужно сохранить неизвестные поля.</li>
 * </ul>
 */
package dev.vida.installer.mc;
