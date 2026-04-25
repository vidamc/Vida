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
@ApiStatus.Stable
public record TransformReport(
        byte[] bytes,
        List<String> appliedMorphs,
        List<VifadaError> errors,
        List<MorphSkip> skippedMorphs) {

    /** @param skippedMorphs может быть {@code null} (трактуется как пустой список) */
    public TransformReport(
            byte[] bytes,
            List<String> appliedMorphs,
            List<VifadaError> errors,
            List<MorphSkip> skippedMorphs) {
        this.appliedMorphs = List.copyOf(Objects.requireNonNull(appliedMorphs, "appliedMorphs"));
        this.errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
        this.skippedMorphs =
                skippedMorphs == null ? List.of() : List.copyOf(skippedMorphs);
        this.bytes = bytes;
    }

    public TransformReport(byte[] bytes, List<String> appliedMorphs, List<VifadaError> errors) {
        this(bytes, appliedMorphs, errors, List.of());
    }

    public boolean isOk()                    { return bytes != null && errors.isEmpty(); }
    public boolean hasErrors()               { return !errors.isEmpty(); }
}
