/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.catalogo;

import dev.vida.core.ApiStatus;
import java.util.Objects;
import java.util.Optional;

/**
 * Handle на зарегистрированную запись в {@link Catalogo}.
 *
 * <p>Связывает {@link CatalogoClave} с фактическим значением и числовым
 * сетевым id. Держит значение лениво — при первом обращении
 * {@link #valor()} читает из реестра (это позволяет возвращать
 * {@link Inscripcion} ещё до фактической регистрации).
 *
 * @param <T> тип значения
 */
@ApiStatus.Preview("base")
public final class Inscripcion<T> {

    private final Catalogo<T> catalogo;
    private final CatalogoClave<T> clave;
    private final int numerico;

    Inscripcion(Catalogo<T> catalogo, CatalogoClave<T> clave, int numerico) {
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
        this.clave = Objects.requireNonNull(clave, "clave");
        if (numerico < 0) throw new IllegalArgumentException("numerico < 0");
        this.numerico = numerico;
    }

    public CatalogoClave<T> clave() { return clave; }
    /** Стабильный сетевой id: номер записи в реестре, назначенный при регистрации. */
    public int numerico()           { return numerico; }

    /** Текущее значение. Пусто, если запись уже была удалена из реестра. */
    public Optional<T> valor() {
        return catalogo.obtener(clave);
    }

    /** Принудительно берёт значение; бросает {@link IllegalStateException} если его нет. */
    public T exigir() {
        return valor().orElseThrow(() ->
                new IllegalStateException("inscripcion " + clave + " has been removed"));
    }

    @Override
    public String toString() {
        return "Inscripcion(" + clave + " #" + numerico + ")";
    }
}
