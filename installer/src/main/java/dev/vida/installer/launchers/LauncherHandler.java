/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers;

import dev.vida.installer.InstallOptions;
import dev.vida.installer.InstallReport;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Контракт установки Vida в конкретный лаунчер Minecraft.
 *
 * <p>Реализации хранятся в {@link LauncherRegistry}; их выбор происходит
 * по {@link LauncherKind} из {@link InstallOptions#launcherKind()}.
 *
 * <h2>Жизненный цикл</h2>
 * <ol>
 *   <li>GUI/CLI вызывает {@link #detectDataDirs()} — autodetect каталога
 *       лаунчера. Если нашлось несколько, пользователю показывается выбор.</li>
 *   <li>Для {@link InstallMode#PATCH_EXISTING_INSTANCE} GUI/CLI вызывает
 *       {@link #listInstances(Path)} чтобы показать список instance'ов.</li>
 *   <li>{@link #install(InstallOptions, Consumer)} выполняет собственно
 *       установку и возвращает {@link InstallReport}.</li>
 * </ol>
 *
 * <p>Handler'ы должны быть stateless и thread-safe.
 */
public interface LauncherHandler {

    /** Какой лаунчер обслуживает этот handler. */
    LauncherKind kind();

    /**
     * Набор поддерживаемых режимов установки.
     *
     * <p>Некоторые лаунчеры (Mojang) поддерживают только
     * {@link InstallMode#CREATE_NEW_PROFILE}; другие (ATLauncher) — только
     * {@link InstallMode#PATCH_EXISTING_INSTANCE}; Prism/MultiMC — оба.
     */
    Set<InstallMode> supportedModes();

    /**
     * Какой режим использовать, если пользователь не указал явно.
     * Обычно тот же, что и {@link #supportedModes()} если он один;
     * иначе — {@link InstallMode#CREATE_NEW_PROFILE}.
     */
    default InstallMode defaultMode() {
        Set<InstallMode> modes = supportedModes();
        if (modes.size() == 1) return modes.iterator().next();
        return InstallMode.CREATE_NEW_PROFILE;
    }

    /**
     * Пытается найти каталог данных лаунчера на текущей системе.
     * Может вернуть несколько кандидатов (e.g. ATLauncher portable +
     * Prism в стандартном месте одновременно). Порядок — предпочтение
     * по убыванию.
     *
     * @return непустой список существующих путей; пустой если ничего не найдено
     */
    List<Path> detectDataDirs();

    /**
     * Перечисляет существующие instance'ы в указанном data-dir.
     * Для лаунчеров без концепции instance (только {@code Mojang}) возвращает
     * список профилей, если они есть.
     *
     * @throws IOException если каталог не читается
     */
    List<InstanceRef> listInstances(Path dataDir) throws IOException;

    /**
     * Выполняет установку.
     *
     * <p>Handler должен:
     * <ul>
     *   <li>извлечь loader.jar в правильное место для данного лаунчера;</li>
     *   <li>подготовить/обновить метаданные (version JSON, instance.cfg, и т. д.);</li>
     *   <li>сообщать прогресс через {@code progress.accept(...)};</li>
     *   <li>при {@code dryRun=true} ничего не писать, но вернуть валидный report
     *       с предполагаемым списком файлов и размером loader'а.</li>
     * </ul>
     *
     * @param options параметры установки
     * @param progress callback для сообщений прогресса (может быть {@code null})
     * @return репорт с диагностикой
     */
    InstallReport install(InstallOptions options, Consumer<String> progress);
}
