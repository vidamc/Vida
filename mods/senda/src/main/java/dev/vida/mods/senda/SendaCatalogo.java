/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.senda;

import dev.vida.base.catalogo.CatalogoClave;
import dev.vida.base.catalogo.CatalogoManejador;
import dev.vida.base.catalogo.CatalogoMutable;
import dev.vida.base.latidos.LatidoBus;
import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр путевых точек мода Senda.
 *
 * <h2>Архитектура</h2>
 * <ul>
 *   <li><b>Активные точки</b> — внутренний {@link ConcurrentHashMap}; поддерживает
 *       полный CRUD (добавить/найти/удалить). Это главное хранилище для
 *       HUD и навигации.</li>
 *   <li><b>{@link CatalogoManejador} реестр</b> — официальный append-only реестр
 *       Vida; каждая добавленная точка регистрируется здесь. Используется для
 *       lookup по числовому id и демонстрации {@code Catalogo}-API. Удалённые
 *       точки остаются в реестре (журнал всех известных точек).</li>
 * </ul>
 *
 * <h2>Events</h2>
 * Добавление → {@link SendaLatidos.PuntoAgregado}; удаление →
 * {@link SendaLatidos.PuntoEliminado}; очистка измерения →
 * {@link SendaLatidos.DimensionLimpiada}.
 */
@ApiStatus.Preview("senda")
public final class SendaCatalogo {

    private final CatalogoManejador catalogos;
    private final LatidoBus bus;
    private final SendaConfig config;

    /** Активные точки: dimension → (nombre → punto). */
    private final ConcurrentHashMap<String, Map<String, PuntoRuta>> activos =
            new ConcurrentHashMap<>();

    public SendaCatalogo(CatalogoManejador catalogos, LatidoBus bus, SendaConfig config) {
        this.catalogos = Objects.requireNonNull(catalogos, "catalogos");
        this.bus       = Objects.requireNonNull(bus,       "bus");
        this.config    = Objects.requireNonNull(config,    "config");
    }

    /**
     * Регистрирует путевую точку.
     *
     * <p>Точка добавляется в активный пул и в официальный {@link CatalogoManejador}
     * (для lookup по числовому id). Испускает {@link SendaLatidos.PuntoAgregado}.
     *
     * @param punto точка для регистрации
     * @throws LimiteAlcanzadoException если активных точек уже {@link SendaConfig#maxPuntosPorDimension()}
     * @throws IllegalArgumentException если точка с таким именем уже существует
     */
    public void registrar(PuntoRuta punto) {
        Objects.requireNonNull(punto, "punto");

        Map<String, PuntoRuta> mapa = mapaParaDimension(punto.dimension());
        synchronized (mapa) {
            if (mapa.size() >= config.maxPuntosPorDimension()) {
                throw new LimiteAlcanzadoException(punto.dimension(), config.maxPuntosPorDimension());
            }
            if (mapa.containsKey(punto.nombre())) {
                throw new IllegalArgumentException(
                        "Ya existe un punto llamado \"" + punto.nombre()
                        + "\" en dimensión \"" + punto.dimension() + "\"");
            }
            mapa.put(punto.nombre(), punto);
        }

        // Регистрируем в официальном Catalogo (append-only, не удаляется)
        CatalogoMutable<PuntoRuta> cat = catalogos.abrir(
                Identifier.of("senda", punto.dimension()), PuntoRuta.class);
        CatalogoClave<PuntoRuta> clave = CatalogoClave.de(
                Identifier.of("senda", punto.dimension()),
                Identifier.of("senda", punto.nombre()));
        cat.registrar(clave, punto); // ошибки дублей игнорируем: дубль уже поймали выше

        bus.emitir(SendaLatidos.PuntoAgregado.TIPO, new SendaLatidos.PuntoAgregado(punto));
    }

    /**
     * Находит активную точку по имени и измерению.
     *
     * @param nombre    имя точки
     * @param dimension измерение
     * @return точка; {@link Optional#empty()} если не найдена или удалена
     */
    public Optional<PuntoRuta> buscar(String nombre, String dimension) {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(dimension, "dimension");
        Map<String, PuntoRuta> mapa = activos.get(dimension);
        if (mapa == null) return Optional.empty();
        return Optional.ofNullable(mapa.get(nombre));
    }

    /**
     * Удаляет активную точку.
     *
     * <p>Точка исчезает из активного пула; в официальном {@link CatalogoManejador}
     * запись остаётся (исторический журнал). Испускает
     * {@link SendaLatidos.PuntoEliminado}.
     *
     * @param nombre    имя точки
     * @param dimension измерение
     * @return удалённая точка; {@link Optional#empty()} если не найдена
     */
    public Optional<PuntoRuta> eliminar(String nombre, String dimension) {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(dimension, "dimension");
        Map<String, PuntoRuta> mapa = activos.get(dimension);
        if (mapa == null) return Optional.empty();
        PuntoRuta eliminado;
        synchronized (mapa) {
            eliminado = mapa.remove(nombre);
        }
        if (eliminado != null) {
            bus.emitir(SendaLatidos.PuntoEliminado.TIPO,
                    new SendaLatidos.PuntoEliminado(eliminado));
        }
        return Optional.ofNullable(eliminado);
    }

    /**
     * Все активные точки для заданного измерения.
     *
     * @param dimension измерение
     * @return неизменяемый снимок активных точек
     */
    public Collection<PuntoRuta> puntosDe(String dimension) {
        Objects.requireNonNull(dimension, "dimension");
        Map<String, PuntoRuta> mapa = activos.get(dimension);
        if (mapa == null) return List.of();
        synchronized (mapa) {
            return Collections.unmodifiableList(new ArrayList<>(mapa.values()));
        }
    }

    /**
     * Количество активных точек для заданного измерения.
     *
     * @param dimension измерение
     * @return количество активных точек
     */
    public int cantidad(String dimension) {
        Objects.requireNonNull(dimension, "dimension");
        Map<String, PuntoRuta> mapa = activos.get(dimension);
        if (mapa == null) return 0;
        return mapa.size();
    }

    /**
     * Сколько точек зарегистрировано в официальном {@link CatalogoManejador}
     * (включая удалённые — полный журнал).
     *
     * @param dimension измерение
     * @return размер официального реестра
     */
    public int tamanioRegistro(String dimension) {
        Objects.requireNonNull(dimension, "dimension");
        return catalogos.abrir(Identifier.of("senda", dimension), PuntoRuta.class)
                .tamanio();
    }

    /**
     * Удаляет все активные точки из заданного измерения.
     *
     * <p>Испускает {@link SendaLatidos.DimensionLimpiada}. Официальный
     * реестр не затрагивается.
     *
     * @param dimension измерение
     */
    public void limpiarDimension(String dimension) {
        Objects.requireNonNull(dimension, "dimension");
        Map<String, PuntoRuta> mapa = activos.get(dimension);
        if (mapa != null) {
            synchronized (mapa) {
                mapa.clear();
            }
        }
        bus.emitir(SendaLatidos.DimensionLimpiada.TIPO,
                new SendaLatidos.DimensionLimpiada(dimension));
    }

    // ---------------------------------------------------------------- helpers

    private Map<String, PuntoRuta> mapaParaDimension(String dimension) {
        return activos.computeIfAbsent(dimension, d -> new LinkedHashMap<>());
    }

    // ---------------------------------------------------------------- exceptions

    /**
     * Бросается, когда достигнут максимальный лимит активных точек для измерения.
     */
    public static final class LimiteAlcanzadoException extends RuntimeException {
        private final String dimension;
        private final int limite;

        public LimiteAlcanzadoException(String dimension, int limite) {
            super("Límite de " + limite + " puntos alcanzado para dimensión \"" + dimension + "\"");
            this.dimension = dimension;
            this.limite    = limite;
        }

        public String dimension() { return dimension; }
        public int limite()       { return limite;    }
    }
}
