/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia.io;

import dev.vida.cartografia.MappingTree;
import dev.vida.core.ApiStatus;
import java.util.List;

/**
 * Результат импорта Proguard-текста с дополнительным отчётом.
 *
 * <p>{@linkplain #unresolvedInternalClassRefsInDescriptors()} — внутренние имена
 * классов {@code Lfoo/Bar;} из дескрипторов полей/методов, для которых в первом
 * проходе не было строки класса в маппинге (часто внешние типы или опечатки).
 */
@ApiStatus.Stable
public record ProguardImportDiagnostics(
        MappingTree tree, List<String> unresolvedInternalClassRefsInDescriptors) {}
