/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.ajustes;

import dev.vida.config.Ajustes;
import dev.vida.config.AjustesLoader;
import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Единый путь «файл TOML → {@link AjustesTipados}» с версией схемы и одноразовой миграцией.
 */
@ApiStatus.Preview("base")
public final class AjustesArchivoGestor {

    private AjustesArchivoGestor() {}

    /**
     * @param migracion если задан и версия на диске ниже целевой — вызывается один раз для raw TOML
     */
    public static Result<AjustesTipados, String> cargar(
            Path archivoToml,
            int esquemaMeta,
            Optional<UnaryOperator<Ajustes>> migracion) {
        Objects.requireNonNull(archivoToml, "archivoToml");
        try {
            if (!Files.isRegularFile(archivoToml)) {
                return Result.ok(AjustesTipados.sobre(Ajustes.empty()));
            }
            String texto = Files.readString(archivoToml);
            Result<Ajustes, dev.vida.config.AjustesError> raw =
                    AjustesLoader.fromToml(archivoToml.getFileName().toString(), texto).build();
            if (raw.isErr()) {
                return Result.err(raw.unwrapErr().toString());
            }
            Ajustes base = raw.unwrap();
            int enDisco = base.getInt("meta.schema", 0);
            if (migracion.isPresent() && enDisco < esquemaMeta) {
                base = migracion.get().apply(base);
            }
            return Result.ok(AjustesTipados.sobre(base));
        } catch (Exception ex) {
            return Result.err(ex.getMessage());
        }
    }
}
