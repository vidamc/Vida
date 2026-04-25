/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.escultores;

import dev.vida.core.ApiStatus;

/**
 * Низкоуровневый трансформер байткода: функция {@code byte[] → byte[]} с
 * дешёвым pre-check.
 */
@ApiStatus.Stable
public interface Escultor {

    /** Имя для логов и диагностики. */
    String nombre();

    /**
     * Быстрый предикат: нужно ли вызывать {@link #tryPatch}. Если {@code false},
     * {@code tryPatch} не вызывается.
     */
    boolean mightMatch(String nombreClaseInternal, byte[] archivoClase);

    /**
     * Применить патч. Возвращает {@code null}, если класс не изменён.
     */
    byte[] tryPatch(String nombreClaseInternal, byte[] archivoClase);
}
