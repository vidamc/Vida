/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.catalogo;

import dev.vida.core.ApiStatus;
import dev.vida.core.Result;

/**
 * Мутабельная фаза реестра. Позволяет {@code registrar(...)} пока реестр
 * не заморожен.
 *
 * <p>Методы <b>thread-safe</b>: шина и базовый реализатор (`DefaultCatalogo`)
 * гарантируют атомарность регистрации.
 */
@ApiStatus.Stable
public interface CatalogoMutable<T> extends Catalogo<T> {

    /**
     * Регистрирует значение.
     *
     * @return успех с {@link Inscripcion}, или одна из {@link CatalogoError}
     *         (дубликат ключа, замороженный реестр, ключ не от того реестра).
     */
    Result<Inscripcion<T>, CatalogoError> registrar(CatalogoClave<T> clave, T valor);

    /**
     * Регистрирует значение и бросает {@link IllegalStateException} при ошибке.
     * Удобно для мест, где сбой регистрации — логический баг мода.
     */
    default Inscripcion<T> registrarOExigir(CatalogoClave<T> clave, T valor) {
        Result<Inscripcion<T>, CatalogoError> r = registrar(clave, valor);
        if (r.isErr()) {
            throw new IllegalStateException(
                    "failed to register " + clave + ": " + r.unwrapErr());
        }
        return r.unwrap();
    }

    /** Замораживает реестр: регистрация дальше возвращает {@link CatalogoError.CatalogoCerrado}. */
    void congelar();
}
