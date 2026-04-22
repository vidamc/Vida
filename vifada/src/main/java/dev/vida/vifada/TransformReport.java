/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import dev.vida.core.ApiStatus;
import java.util.List;
import java.util.Objects;

/**
 * Итоговый отчёт одной трансформации: байты результата + список
 * применённых морфов и накопленные ошибки.
 *
 * <p>{@link #bytes()} — {@code null}, если трансформация была прервана
 * ошибкой, и {@code non-null} даже при частичном успехе (когда часть
 * морфов не удалось применить, но это были «silent» варианты).
 */
@ApiStatus.Preview("vifada")
public record TransformReport(
        byte[] bytes,
        List<String> appliedMorphs,
        List<VifadaError> errors) {

    public TransformReport {
        appliedMorphs = List.copyOf(Objects.requireNonNull(appliedMorphs, "appliedMorphs"));
        errors        = List.copyOf(Objects.requireNonNull(errors, "errors"));
    }

    public boolean isOk()                    { return bytes != null && errors.isEmpty(); }
    public boolean hasErrors()               { return !errors.isEmpty(); }
}
