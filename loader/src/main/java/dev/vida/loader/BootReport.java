/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.core.ApiStatus;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Итог одного бутстрапа.
 *
 * <p>Содержит собранное {@link VidaEnvironment} (если удалось собрать),
 * список {@link LoaderError} и общую длительность. {@link #isOk()} —
 * true, только если окружение создано и ошибок нет.
 */
@ApiStatus.Preview("loader")
public record BootReport(VidaEnvironment environment,
                         List<LoaderError> errors,
                         Duration duration) {

    public BootReport {
        errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
        Objects.requireNonNull(duration, "duration");
    }

    public boolean isOk() { return environment != null && errors.isEmpty(); }
    public boolean hasErrors() { return !errors.isEmpty(); }

    public Optional<VidaEnvironment> environmentOpt() {
        return Optional.ofNullable(environment);
    }
}
