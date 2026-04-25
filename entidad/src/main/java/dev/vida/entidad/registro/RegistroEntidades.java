/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.entidad.registro;

import dev.vida.base.catalogo.Catalogo;
import dev.vida.base.catalogo.CatalogoClave;
import dev.vida.base.catalogo.CatalogoError;
import dev.vida.base.catalogo.CatalogoManejador;
import dev.vida.base.catalogo.CatalogoMutable;
import dev.vida.base.catalogo.Inscripcion;
import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import dev.vida.core.Result;
import dev.vida.entidad.Entidad;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Регистр entity-type'ов.
 */
@ApiStatus.Stable
public final class RegistroEntidades {

    public static final Identifier CATALOGO_ID = Identifier.of("vida", "entidad");

    private final CatalogoMutable<Entidad> catalogo;
    private final String namespacePorDefecto;

    private RegistroEntidades(CatalogoMutable<Entidad> catalogo, String namespacePorDefecto) {
        this.catalogo = catalogo;
        this.namespacePorDefecto = namespacePorDefecto;
    }

    public static RegistroEntidades conectar(CatalogoManejador manejador, String namespacePorDefecto) {
        Objects.requireNonNull(manejador, "manejador");
        Objects.requireNonNull(namespacePorDefecto, "namespacePorDefecto");
        CatalogoMutable<Entidad> cat = manejador.abrir(CATALOGO_ID, Entidad.class);
        return new RegistroEntidades(cat, namespacePorDefecto);
    }

    public Result<Inscripcion<Entidad>, CatalogoError> registrar(Entidad entidad) {
        Objects.requireNonNull(entidad, "entidad");
        return catalogo.registrar(CatalogoClave.de(CATALOGO_ID, entidad.id()), entidad);
    }

    public Inscripcion<Entidad> registrarOExigir(Entidad entidad) {
        Result<Inscripcion<Entidad>, CatalogoError> r = registrar(entidad);
        if (r.isErr()) {
            throw new IllegalStateException(
                    "не удалось зарегистрировать сущность " + entidad.id()
                            + ": " + r.unwrapErr());
        }
        return r.unwrap();
    }

    public Catalogo<Entidad> catalogo() { return catalogo; }
    public int cantidad() { return catalogo.tamanio(); }

    public Optional<Entidad> obtener(Identifier id) {
        return catalogo.obtener(CatalogoClave.de(CATALOGO_ID, id));
    }

    public Optional<Entidad> obtener(String path) {
        return obtener(Identifier.of(namespacePorDefecto, path));
    }

    public Collection<Entidad> todos() { return catalogo.valores(); }

    public String namespacePorDefecto() { return namespacePorDefecto; }

    public void congelar() { catalogo.congelar(); }
    public boolean congelado() { return catalogo.congelado(); }
}
