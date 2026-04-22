/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers.multimc;

import dev.vida.installer.launchers.LauncherKind;
import dev.vida.installer.launchers.prism.PrismLikeHandler;
import java.nio.file.Path;
import java.util.List;

/**
 * Handler для MultiMC.
 *
 * <p>MultiMC — portable-by-default, data-dir совпадает с каталогом
 * {@code MultiMC.exe}/{@code MultiMC} на диске. Мы не пытаемся его
 * угадать: пользователь должен указать путь явно через GUI или
 * {@code --dir}.
 *
 * <p>Формат instance'а идентичен Prism, но MultiMC не знает про
 * component {@code +agents}, поэтому {@code -javaagent} инжектируется
 * через {@code JvmArgs} в {@code instance.cfg}.
 */
public final class MultiMCHandler extends PrismLikeHandler {

    @Override
    public LauncherKind kind() { return LauncherKind.MULTIMC; }

    @Override
    protected boolean supportsAgents() { return false; }

    @Override
    public List<Path> detectDataDirs() {
        // MultiMC — portable, единого «стандартного» места нет.
        // Пусть пользователь выбирает каталог вручную.
        return List.of();
    }
}
