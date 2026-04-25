/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import dev.vida.core.ApiStatus;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marco documental para «Latidos profundos»: выбор потока исполнения через
 * {@link EjecutorLatido}.
 *
 * <p>Puede usarse junto con {@link EjecutorLatido}; no añade reglas nuevas —
 * etiqueta métodos gestionados por {@link LatidoRegistrador}.
 *
 * @see Ejecutor
 */
@ApiStatus.Stable
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LatidosProfundos {
    /** Descripción opcional para documentación estática del mod. */
    String value() default "";
}
