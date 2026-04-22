/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import dev.vida.core.ApiStatus;

/**
 * Места, куда можно вставить инъекцию.
 *
 * <p>MVP поддерживает {@link #HEAD} и {@link #RETURN}. Остальные значения
 * объявлены, но в P7 ещё не реализованы; попытка их использовать даст
 * {@link VifadaError.UnsupportedAt}.
 */
@ApiStatus.Preview("vifada")
public enum InjectionPoint {

    /** Самое начало метода, до первой пользовательской инструкции. */
    HEAD,

    /**
     * Перед каждой инструкцией возврата ({@code RETURN}, {@code IRETURN},
     * {@code ARETURN} и т.д.) в теле метода.
     */
    RETURN,

    /** Зарезервировано: вызов конкретного метода. */
    INVOKE,

    /** Зарезервировано: перед чтением/записью поля. */
    FIELD,

    /** Зарезервировано: указанная константа в пуле. */
    CONSTANT,

    /** Зарезервировано: начало блока цикла / метка. */
    JUMP
}
