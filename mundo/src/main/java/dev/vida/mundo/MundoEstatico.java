/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;
import java.util.Optional;

/**
 * Минимальная реализация {@link Mundo} для мостов платформы (тик клиента и т.п.).
 */
@ApiStatus.Stable
public final class MundoEstatico implements Mundo {

    private final Identifier id;
    private final Dimension dimension;
    private final long tiempoDelDia;
    private final Bioma biomaPorDefecto;
    private final boolean chunkCargado;
    private final LimitesVerticales limitesExplicitos;
    private final Identifier bloqueRegistradoFijo;

    public MundoEstatico(Identifier id, Dimension dimension, long tiempoDelDia) {
        this(id, dimension, tiempoDelDia, biomaPlainsPorDefecto(), false, null, null);
    }

    /**
     * @param biomaPorDefecto ответ {@link #biomaEn(Coordenada)} для любой координаты
     * @param chunkCargado      ответ {@link #estaCargado(Coordenada)}
     */
    public MundoEstatico(
            Identifier id,
            Dimension dimension,
            long tiempoDelDia,
            Bioma biomaPorDefecto,
            boolean chunkCargado) {
        this(id, dimension, tiempoDelDia, biomaPorDefecto, chunkCargado, null, null);
    }

    /**
     * @param limitesExplicitos если не {@code null}, возвращаются из {@link #limitesVerticales()}
     */
    public MundoEstatico(
            Identifier id,
            Dimension dimension,
            long tiempoDelDia,
            Bioma biomaPorDefecto,
            boolean chunkCargado,
            LimitesVerticales limitesExplicitos) {
        this(id, dimension, tiempoDelDia, biomaPorDefecto, chunkCargado, limitesExplicitos, null);
    }

    /**
     * @param bloqueRegistradoFijo если не {@code null}, {@link #bloqueRegistradoEn(Coordenada)} стабильно
     *                             возвращает его (для тестов и заглушек без координатной карты).
     */
    public MundoEstatico(
            Identifier id,
            Dimension dimension,
            long tiempoDelDia,
            Bioma biomaPorDefecto,
            boolean chunkCargado,
            LimitesVerticales limitesExplicitos,
            Identifier bloqueRegistradoFijo) {
        this.id = Objects.requireNonNull(id, "id");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.tiempoDelDia = tiempoDelDia;
        this.biomaPorDefecto = Objects.requireNonNull(biomaPorDefecto, "biomaPorDefecto");
        this.chunkCargado = chunkCargado;
        this.limitesExplicitos = limitesExplicitos;
        this.bloqueRegistradoFijo = bloqueRegistradoFijo;
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public Dimension dimension() {
        return dimension;
    }

    @Override
    public Bioma biomaEn(Coordenada coordenada) {
        Objects.requireNonNull(coordenada, "coordenada");
        return biomaPorDefecto;
    }

    @Override
    public boolean estaCargado(Coordenada coordenada) {
        Objects.requireNonNull(coordenada, "coordenada");
        return chunkCargado;
    }

    @Override
    public long tiempoDelDia() {
        return tiempoDelDia;
    }

    @Override
    public LimitesVerticales limitesVerticales() {
        return limitesExplicitos != null ? limitesExplicitos : dimension().limitesVerticalesPredeterminados();
    }

    @Override
    public Optional<Identifier> bloqueRegistradoEn(Coordenada coordenada) {
        Objects.requireNonNull(coordenada, "coordenada");
        return Optional.ofNullable(bloqueRegistradoFijo);
    }

    private static Bioma biomaPlainsPorDefecto() {
        return new Bioma(
                Identifier.of("minecraft", "plains"),
                0.5f,
                0.5f,
                Bioma.Precipitacion.NINGUNA);
    }
}
