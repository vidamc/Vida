/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Итог работы {@link InstallerCore#install(InstallOptions)}.
 *
 * <p>При успехе {@link #errors()} пуст, {@link #installedFiles()} содержит
 * все файлы, которые инсталлятор создал/перезаписал. При ошибке — наоборот.
 */
public record InstallReport(
        InstallOptions options,
        Instant timestamp,
        List<Path> installedFiles,
        List<String> warnings,
        List<String> errors,
        long loaderBytes) {

    public InstallReport {
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(timestamp, "timestamp");
        installedFiles = List.copyOf(installedFiles);
        warnings       = List.copyOf(warnings);
        errors         = List.copyOf(errors);
    }

    public boolean isOk() {
        return errors.isEmpty();
    }
}
