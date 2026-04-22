/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.core.ApiStatus;
import dev.vida.resolver.ResolverError;
import dev.vida.vifada.VifadaError;
import java.util.List;
import java.util.Objects;

/** Структурированные ошибки бутстрапа. */
@ApiStatus.Preview("loader")
public sealed interface LoaderError {

    /** Ошибка I/O при чтении/сканировании модов. */
    record IoFailure(String path, String message) implements LoaderError {
        public IoFailure { Objects.requireNonNull(path); Objects.requireNonNull(message); }
    }

    /** Не удалось разобрать манифест у найденного мода. */
    record BadManifest(String source, String message) implements LoaderError {
        public BadManifest { Objects.requireNonNull(source); Objects.requireNonNull(message); }
    }

    /** Резолвер не смог удовлетворить зависимости — обёртка вокруг {@link ResolverError}. */
    record ResolutionFailed(ResolverError cause) implements LoaderError {
        public ResolutionFailed { Objects.requireNonNull(cause); }
    }

    /** Ошибка разбора/применения морфа — агрегат ошибок Vifada. */
    record VifadaFailed(String morphSource, List<VifadaError> errors) implements LoaderError {
        public VifadaFailed {
            Objects.requireNonNull(morphSource);
            errors = List.copyOf(Objects.requireNonNull(errors, "errors"));
        }
    }

    /** Ошибка data-driven прототипа (разбор datapack/manifest custom). */
    record DataDrivenFailure(String source, String message) implements LoaderError {
        public DataDrivenFailure {
            Objects.requireNonNull(source);
            Objects.requireNonNull(message);
        }
    }

    /** Некорректные параметры запуска. */
    record InvalidOptions(String message) implements LoaderError {
        public InvalidOptions { Objects.requireNonNull(message); }
    }

    /** Общая ошибка верхнего уровня, которую не удалось классифицировать. */
    record BootFailure(String message) implements LoaderError {
        public BootFailure { Objects.requireNonNull(message); }
    }
}
