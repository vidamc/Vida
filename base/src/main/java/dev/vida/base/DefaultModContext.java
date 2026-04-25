/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base;

import dev.vida.base.ajustes.AjustesTipados;
import dev.vida.base.catalogo.CatalogoManejador;
import dev.vida.base.latidos.LatidoBus;
import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import dev.vida.core.Version;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Базовая реализация {@link ModContext} — иммутабельная value-обёртка.
 *
 * <p>Применяется и из {@code :loader} (при бутстрапе), и в unit-тестах
 * моддеров, которые хотят собрать контекст вручную, не поднимая всю Vida.
 */
@ApiStatus.Stable
public final class DefaultModContext implements ModContext {

    private final ModMetadata metadata;
    private final LatidoBus latidos;
    private final CatalogoManejador catalogos;
    private final AjustesTipados ajustes;
    private final Log log;
    private final Path directorioDatos;

    public DefaultModContext(
            ModMetadata metadata,
            LatidoBus latidos,
            CatalogoManejador catalogos,
            AjustesTipados ajustes,
            Log log,
            Path directorioDatos) {
        this.metadata        = Objects.requireNonNull(metadata, "metadata");
        this.latidos         = Objects.requireNonNull(latidos, "latidos");
        this.catalogos       = Objects.requireNonNull(catalogos, "catalogos");
        this.ajustes         = Objects.requireNonNull(ajustes, "ajustes");
        this.log             = Objects.requireNonNull(log, "log");
        this.directorioDatos = Objects.requireNonNull(directorioDatos, "directorioDatos");
    }

    @Override public String id()                       { return metadata.id(); }
    @Override public Version version()                 { return metadata.version(); }
    @Override public ModMetadata metadata()            { return metadata; }
    @Override public LatidoBus latidos()               { return latidos; }
    @Override public CatalogoManejador catalogos()     { return catalogos; }
    @Override public AjustesTipados ajustes()          { return ajustes; }
    @Override public Log log()                         { return log; }
    @Override public Path directorioDatos()            { return directorioDatos; }

    @Override
    public String toString() {
        return "ModContext(" + metadata.id() + "@" + metadata.version() + ")";
    }
}
