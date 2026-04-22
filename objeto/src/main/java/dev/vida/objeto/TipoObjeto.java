/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.objeto;

import dev.vida.core.ApiStatus;

/**
 * Тип предмета — определяет базовый набор поведений.
 *
 * <p>{@link #BLOQUE} используется для BlockItem'ов (то, что ставится в мир
 * и соответствует блоку). {@link #HERRAMIENTA} — для предметов-инструментов.
 * {@link #CONSUMIBLE} — еда и напитки. {@link #ARMADURA} — броня. Всё
 * остальное — {@link #GENERICO}.
 */
@ApiStatus.Preview("objeto")
public enum TipoObjeto {
    GENERICO,
    BLOQUE,
    HERRAMIENTA,
    ARMA,
    ARMADURA,
    CONSUMIBLE,
    CONTENEDOR,
    PROYECTIL,
    MATERIAL
}
