/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.catalogo;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Collection;
import java.util.Optional;

/**
 * Read-only вид на реестр.
 *
 * <p>Типичный жизненный цикл:
 * <ol>
 *   <li>{@link CatalogoManejador#abrir} — создаёт новый реестр в режиме
 *       регистрации ({@link CatalogoMutable});</li>
 *   <li>моды наполняют его {@code registrar(...)};</li>
 *   <li>в определённый момент ядро «замораживает» реестр, и дальше он
 *       доступен только через {@link Catalogo}.</li>
 * </ol>
 *
 * <p>Методы чтения безопасны для вызова из любого потока — реализация
 * обеспечивает иммутабельное внутреннее представление после freeze.
 *
 * @param <T> тип значений
 */
@ApiStatus.Stable
public interface Catalogo<T> {

    /** Идентификатор самого реестра. */
    Identifier reestroId();

    /** Тип значений. */
    Class<T> claseValor();

    /** Заморожен ли реестр — можно ли ещё регистрировать записи. */
    boolean congelado();

    /** Количество записей. */
    int tamanio();

    /** {@code true}, если в реестре есть запись по этому ключу. */
    boolean contiene(CatalogoClave<T> clave);

    /** Значение или {@link Optional#empty()}. */
    Optional<T> obtener(CatalogoClave<T> clave);

    /** Значение по числовому id. */
    Optional<T> obtener(int numerico);

    /** Числовой id записи. */
    Optional<Integer> numericoDe(CatalogoClave<T> clave);

    /** Все ключи в стабильном порядке регистрации. */
    Collection<CatalogoClave<T>> claves();

    /** Все значения в стабильном порядке регистрации. */
    Collection<T> valores();

    /** Все inscripciones в стабильном порядке регистрации. */
    Collection<Inscripcion<T>> inscripciones();
}
