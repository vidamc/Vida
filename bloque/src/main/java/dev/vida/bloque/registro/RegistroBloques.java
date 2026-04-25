/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque.registro;

import dev.vida.base.catalogo.Catalogo;
import dev.vida.base.catalogo.CatalogoClave;
import dev.vida.base.catalogo.CatalogoError;
import dev.vida.base.catalogo.CatalogoManejador;
import dev.vida.base.catalogo.CatalogoMutable;
import dev.vida.base.catalogo.Inscripcion;
import dev.vida.bloque.Bloque;
import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import dev.vida.core.Result;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Регистр блоков и их тегов.
 *
 * <p>Построен над общим {@link CatalogoManejador} из {@code vida-base}
 * — это значит, что все блоки видны через те же реестры, что и остальной
 * контент мода.
 *
 * <h2>Использование</h2>
 * <pre>{@code
 *   RegistroBloques reg = RegistroBloques.conectar(ctx.catalogos(), "ejemplo");
 *
 *   Bloque piedraOscura = new Bloque(
 *           Identifier.of("ejemplo", "piedra_oscura"),
 *           PropiedadesBloque.con(MaterialBloque.PIEDRA)
 *                   .dureza(2.0f)
 *                   .herramientas(TipoHerramienta.PICO)
 *                   .construir());
 *
 *   reg.registrar(piedraOscura, EtiquetaBloque.de("vida", "mineable/pico"));
 * }</pre>
 */
@ApiStatus.Stable
public final class RegistroBloques {

    /** ID общего Catalogo блоков Vida. */
    public static final Identifier CATALOGO_ID = Identifier.of("vida", "bloque");

    private final CatalogoMutable<Bloque> catalogo;
    private final String namespacePorDefecto;
    private final ConcurrentHashMap<Identifier, Set<Identifier>> tagToBlocks = new ConcurrentHashMap<>();

    private RegistroBloques(CatalogoMutable<Bloque> catalogo, String namespacePorDefecto) {
        this.catalogo = catalogo;
        this.namespacePorDefecto = namespacePorDefecto;
    }

    /**
     * Подключает регистр к существующему {@link CatalogoManejador}. Если
     * общий catalogo ещё не открыт, он создаётся. Повторный вызов с тем же
     * manager'ом возвращает обёртку над тем же catalogo.
     *
     * @param manejador          общий реестр реестров
     * @param namespacePorDefecto namespace по умолчанию для сокращённых регистраций
     */
    public static RegistroBloques conectar(CatalogoManejador manejador, String namespacePorDefecto) {
        Objects.requireNonNull(manejador, "manejador");
        Objects.requireNonNull(namespacePorDefecto, "namespacePorDefecto");
        CatalogoMutable<Bloque> cat = manejador.abrir(CATALOGO_ID, Bloque.class);
        return new RegistroBloques(cat, namespacePorDefecto);
    }

    // ------------------------------------------------------------------ register

    /** Регистрирует блок под его собственным id. */
    public Result<Inscripcion<Bloque>, CatalogoError> registrar(Bloque bloque) {
        Objects.requireNonNull(bloque, "bloque");
        CatalogoClave<Bloque> clave = CatalogoClave.de(CATALOGO_ID, bloque.id());
        return catalogo.registrar(clave, bloque);
    }

    /** Регистрирует блок и добавляет его во все указанные теги. */
    public Result<Inscripcion<Bloque>, CatalogoError> registrar(Bloque bloque,
                                                                EtiquetaBloque... etiquetas) {
        Objects.requireNonNull(etiquetas, "etiquetas");
        Result<Inscripcion<Bloque>, CatalogoError> r = registrar(bloque);
        if (r.isOk()) {
            for (EtiquetaBloque e : etiquetas) {
                Objects.requireNonNull(e, "etiqueta");
                etiquetar(bloque.id(), e);
            }
        }
        return r;
    }

    /**
     * Регистрирует блок или бросает {@link IllegalStateException}.
     * Удобно для мест, где провал — логический баг.
     */
    public Inscripcion<Bloque> registrarOExigir(Bloque bloque, EtiquetaBloque... etiquetas) {
        Result<Inscripcion<Bloque>, CatalogoError> r = registrar(bloque, etiquetas);
        if (r.isErr()) {
            throw new IllegalStateException(
                    "не удалось зарегистрировать блок " + bloque.id()
                            + ": " + r.unwrapErr());
        }
        return r.unwrap();
    }

    // ------------------------------------------------------------------ tags

    /** Добавляет блок в тег. Блок не обязан быть уже зарегистрирован — это
     *  допустимо (тег может быть «декларацией» до фактической регистрации). */
    public void etiquetar(Identifier bloque, EtiquetaBloque etiqueta) {
        Objects.requireNonNull(bloque, "bloque");
        Objects.requireNonNull(etiqueta, "etiqueta");
        tagToBlocks.computeIfAbsent(etiqueta.id(),
                id -> Collections.synchronizedSet(new LinkedHashSet<>())).add(bloque);
    }

    /** Добавляет уже существующий блок в тег. */
    public void etiquetar(Bloque bloque, EtiquetaBloque etiqueta) {
        etiquetar(bloque.id(), etiqueta);
    }

    /** Блоки тега (immutable snapshot). */
    public Set<Identifier> miembros(EtiquetaBloque etiqueta) {
        Objects.requireNonNull(etiqueta, "etiqueta");
        Set<Identifier> s = tagToBlocks.get(etiqueta.id());
        if (s == null) return Set.of();
        synchronized (s) {
            return Set.copyOf(s);
        }
    }

    /** Проверка членства. */
    public boolean contiene(EtiquetaBloque etiqueta, Identifier bloque) {
        Objects.requireNonNull(etiqueta, "etiqueta");
        Objects.requireNonNull(bloque, "bloque");
        Set<Identifier> s = tagToBlocks.get(etiqueta.id());
        if (s == null) return false;
        synchronized (s) {
            return s.contains(bloque);
        }
    }

    /** Карта всех известных тегов → их членов. */
    public Map<Identifier, Set<Identifier>> snapshotEtiquetas() {
        Map<Identifier, Set<Identifier>> out = new java.util.LinkedHashMap<>();
        for (var e : tagToBlocks.entrySet()) {
            synchronized (e.getValue()) {
                out.put(e.getKey(), Set.copyOf(e.getValue()));
            }
        }
        return Map.copyOf(out);
    }

    // ------------------------------------------------------------------ read

    /** Read-only view. */
    public Catalogo<Bloque> catalogo() { return catalogo; }

    /** Количество зарегистрированных блоков. */
    public int cantidad() { return catalogo.tamanio(); }

    /** Блок по id. */
    public Optional<Bloque> obtener(Identifier id) {
        return catalogo.obtener(CatalogoClave.de(CATALOGO_ID, id));
    }

    /** Блок по короткому path (namespace берётся из {@link #namespacePorDefecto}). */
    public Optional<Bloque> obtener(String path) {
        return obtener(Identifier.of(namespacePorDefecto, path));
    }

    /** Все зарегистрированные блоки. */
    public Collection<Bloque> todos() { return catalogo.valores(); }

    /** namespace, используемый для сокращённых путей. */
    public String namespacePorDefecto() { return namespacePorDefecto; }

    /** Заморозить все операции регистрации. */
    public void congelar() { catalogo.congelar(); }

    /** Заморожен ли реестр. */
    public boolean congelado() { return catalogo.congelado(); }
}
