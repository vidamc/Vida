/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.objeto.registro;

import dev.vida.base.catalogo.Catalogo;
import dev.vida.base.catalogo.CatalogoClave;
import dev.vida.base.catalogo.CatalogoError;
import dev.vida.base.catalogo.CatalogoManejador;
import dev.vida.base.catalogo.CatalogoMutable;
import dev.vida.base.catalogo.Inscripcion;
import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import dev.vida.core.Result;
import dev.vida.objeto.Objeto;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Регистр предметов.
 *
 * <p>Устроен симметрично {@link dev.vida.bloque.registro.RegistroBloques}:
 * обёртка над {@link CatalogoManejador}, типизированная по {@link Objeto}.
 * Дополнительно ведёт список тегов.
 */
@ApiStatus.Stable
public final class RegistroObjetos {

    /** id общего реестра предметов Vida. */
    public static final Identifier CATALOGO_ID = Identifier.of("vida", "objeto");

    private final CatalogoMutable<Objeto> catalogo;
    private final String namespacePorDefecto;
    private final ConcurrentHashMap<Identifier, Set<Identifier>> tagToItems = new ConcurrentHashMap<>();

    private RegistroObjetos(CatalogoMutable<Objeto> catalogo, String namespacePorDefecto) {
        this.catalogo = catalogo;
        this.namespacePorDefecto = namespacePorDefecto;
    }

    /** Подключиться к общему manejador'у. */
    public static RegistroObjetos conectar(CatalogoManejador manejador, String namespacePorDefecto) {
        Objects.requireNonNull(manejador, "manejador");
        Objects.requireNonNull(namespacePorDefecto, "namespacePorDefecto");
        CatalogoMutable<Objeto> cat = manejador.abrir(CATALOGO_ID, Objeto.class);
        return new RegistroObjetos(cat, namespacePorDefecto);
    }

    // ------------------------------------------------------------------

    public Result<Inscripcion<Objeto>, CatalogoError> registrar(Objeto objeto) {
        Objects.requireNonNull(objeto, "objeto");
        return catalogo.registrar(CatalogoClave.de(CATALOGO_ID, objeto.id()), objeto);
    }

    public Result<Inscripcion<Objeto>, CatalogoError> registrar(Objeto objeto,
                                                                EtiquetaObjeto... etiquetas) {
        Objects.requireNonNull(etiquetas, "etiquetas");
        Result<Inscripcion<Objeto>, CatalogoError> r = registrar(objeto);
        if (r.isOk()) {
            for (EtiquetaObjeto e : etiquetas) {
                Objects.requireNonNull(e, "etiqueta");
                etiquetar(objeto.id(), e);
            }
        }
        return r;
    }

    public Inscripcion<Objeto> registrarOExigir(Objeto objeto, EtiquetaObjeto... etiquetas) {
        Result<Inscripcion<Objeto>, CatalogoError> r = registrar(objeto, etiquetas);
        if (r.isErr()) {
            throw new IllegalStateException(
                    "не удалось зарегистрировать объект " + objeto.id()
                            + ": " + r.unwrapErr());
        }
        return r.unwrap();
    }

    public void etiquetar(Identifier objeto, EtiquetaObjeto etiqueta) {
        Objects.requireNonNull(objeto, "objeto");
        Objects.requireNonNull(etiqueta, "etiqueta");
        tagToItems.computeIfAbsent(etiqueta.id(),
                id -> Collections.synchronizedSet(new LinkedHashSet<>())).add(objeto);
    }

    public void etiquetar(Objeto objeto, EtiquetaObjeto etiqueta) {
        etiquetar(objeto.id(), etiqueta);
    }

    public Set<Identifier> miembros(EtiquetaObjeto etiqueta) {
        Objects.requireNonNull(etiqueta, "etiqueta");
        Set<Identifier> s = tagToItems.get(etiqueta.id());
        if (s == null) return Set.of();
        synchronized (s) {
            return Set.copyOf(s);
        }
    }

    public boolean contiene(EtiquetaObjeto etiqueta, Identifier objeto) {
        Objects.requireNonNull(etiqueta, "etiqueta");
        Objects.requireNonNull(objeto, "objeto");
        Set<Identifier> s = tagToItems.get(etiqueta.id());
        if (s == null) return false;
        synchronized (s) {
            return s.contains(objeto);
        }
    }

    public Map<Identifier, Set<Identifier>> snapshotEtiquetas() {
        Map<Identifier, Set<Identifier>> out = new java.util.LinkedHashMap<>();
        for (var e : tagToItems.entrySet()) {
            synchronized (e.getValue()) {
                out.put(e.getKey(), Set.copyOf(e.getValue()));
            }
        }
        return Map.copyOf(out);
    }

    public Catalogo<Objeto> catalogo() { return catalogo; }
    public int cantidad() { return catalogo.tamanio(); }

    public Optional<Objeto> obtener(Identifier id) {
        return catalogo.obtener(CatalogoClave.de(CATALOGO_ID, id));
    }

    public Optional<Objeto> obtener(String path) {
        return obtener(Identifier.of(namespacePorDefecto, path));
    }

    public Collection<Objeto> todos() { return catalogo.valores(); }

    public String namespacePorDefecto() { return namespacePorDefecto; }

    public void congelar() { catalogo.congelar(); }
    public boolean congelado() { return catalogo.congelado(); }
}
