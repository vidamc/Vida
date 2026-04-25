/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo;

import dev.vida.bloque.registro.EtiquetaBloque;
import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;
import java.util.Optional;

/**
 * Публичный контракт снимка мира без ссылок на классы игры.
 *
 * <p>Обязательные методы достаточны для мостов и тестов; дополнительные {@code default}-методы
 * расширяются только совместимо по SemVer (предпочтительно через {@code default} в этом интерфейсе).
 */
@ApiStatus.Stable
public interface Mundo {

    Identifier id();

    Dimension dimension();

    Bioma biomaEn(Coordenada coordenada);

    boolean estaCargado(Coordenada coordenada);

    long tiempoDelDia();

    /**
     * Допустимые координаты блока по Y для этого снимка. По умолчанию — из {@link Dimension#limitesVerticalesPredeterminados()}.
     */
    default LimitesVerticales limitesVerticales() {
        return dimension().limitesVerticalesPredeterminados();
    }

    /**
     * Проверка, что {@link Coordenada#y()} попадает в {@link #limitesVerticales()}.
     */
    default boolean enRangoDeAltura(Coordenada coordenada) {
        Objects.requireNonNull(coordenada, "coordenada");
        return limitesVerticales().contiene(coordenada);
    }

    /**
     * Идентификатор блока в реестре игры (пространство имён Mojang↔Vida через мост).
     *
     * <p>{@link Optional#empty()} — чанк не загружен, координата вне мира или снимок не умеет
     * выборку (например тестовый заглушечный {@link Mundo}).
     */
    default Optional<Identifier> bloqueRegistradoEn(Coordenada coordenada) {
        Objects.requireNonNull(coordenada, "coordenada");
        return Optional.empty();
    }

    /**
     * Принадлежность блока в этой клетке тегу блоков (datapack / registry sync).
     *
     * <p>По умолчанию {@code false}. Платформенный мост может делегировать в vanilla {@code BlockState#is(TagKey)}.
     */
    default boolean bloqueTieneEtiqueta(Coordenada coordenada, EtiquetaBloque etiqueta) {
        Objects.requireNonNull(coordenada, "coordenada");
        Objects.requireNonNull(etiqueta, "etiqueta");
        return false;
    }

    default boolean esDeDia() {
        long tiempo = tiempoDelDia() % 24000L;
        return tiempo >= 0L && tiempo < 12000L;
    }

    default boolean esDeNoche() {
        return !esDeDia();
    }
}
