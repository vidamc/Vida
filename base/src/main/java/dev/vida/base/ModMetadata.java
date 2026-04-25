/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base;

import dev.vida.core.ApiStatus;
import dev.vida.core.Version;
import java.util.List;
import java.util.Objects;

/**
 * Иммутабельный срез публичных метаданных мода, доступный в рантайме.
 *
 * <p>Это усечённая «рантайм-версия» {@code ModManifest}: туда, где моды
 * смотрят на чужие моды, {@link dev.vida.manifest.ModManifest} напрямую не
 * передаётся — слишком большой контракт. Вместо него передаём
 * {@link ModMetadata}.
 *
 * @param id          идентификатор мода (как в {@code vida.mod.json:id})
 * @param version     версия мода
 * @param nombre      человекочитаемое имя (может совпадать с {@code id})
 * @param descripcion краткое описание (может быть пустым)
 * @param autores     список авторов; стабильный порядок
 */
@ApiStatus.Stable
public record ModMetadata(
        String id,
        Version version,
        String nombre,
        String descripcion,
        List<String> autores) {

    public ModMetadata {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(descripcion, "descripcion");
        autores = List.copyOf(Objects.requireNonNull(autores, "autores"));
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("nombre must not be blank");
        }
    }

    /** Упрощённая фабрика без авторов/описания. */
    public static ModMetadata basico(String id, Version version) {
        return new ModMetadata(id, version, id, "", List.of());
    }

    /** Упрощённая фабрика с именем. */
    public static ModMetadata con(String id, Version version, String nombre) {
        return new ModMetadata(id, version, nombre, "", List.of());
    }
}
