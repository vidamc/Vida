/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.puertas;

import dev.vida.core.ApiStatus;
import java.util.Objects;
import java.util.Optional;

/**
 * Одна директива в .ptr-файле.
 *
 * <p>Каноническая форма:
 *
 * <pre>
 *   accesible  class   net/example/Foo
 *   accesible  method  net/example/Foo   bar  (I)Ljava/lang/String;
 *   accesible  field   net/example/Foo   count I
 *   extensible class   net/example/Foo
 *   mutable    field   net/example/Foo   count I
 * </pre>
 *
 * <p>Для целей типа {@link Objetivo#CLASE} поля {@link #nombreMiembro} и
 * {@link #descriptor} пусты. Для {@link Objetivo#METODO}/{@link Objetivo#CAMPO}
 * они обязательны.
 *
 * @param accion         какое изменение применяется
 * @param objetivo       класс / метод / поле
 * @param claseInternal  internal-name целевого класса ({@code net/minecraft/world/Foo})
 * @param nombreMiembro  имя метода/поля, или {@link Optional#empty()} для CLASE
 * @param descriptor     JVM-descriptor члена, или {@link Optional#empty()} для CLASE
 * @param linea          номер строки в исходнике (1-based) — для сообщений об ошибках
 */
@ApiStatus.Stable
public record PuertaDirectiva(
        Accion accion,
        Objetivo objetivo,
        String claseInternal,
        Optional<String> nombreMiembro,
        Optional<String> descriptor,
        int linea) {

    public PuertaDirectiva {
        Objects.requireNonNull(accion, "accion");
        Objects.requireNonNull(objetivo, "objetivo");
        Objects.requireNonNull(claseInternal, "claseInternal");
        Objects.requireNonNull(nombreMiembro, "nombreMiembro");
        Objects.requireNonNull(descriptor, "descriptor");
        if (claseInternal.isBlank()) {
            throw new IllegalArgumentException("claseInternal пустой");
        }
        if (claseInternal.contains(".")) {
            throw new IllegalArgumentException(
                    "claseInternal должен быть в internal-name форме (with /): " + claseInternal);
        }
        if (objetivo == Objetivo.CLASE) {
            if (nombreMiembro.isPresent() || descriptor.isPresent()) {
                throw new IllegalArgumentException(
                        "для объективо=CLASE нельзя задавать miembro/descriptor");
            }
        } else {
            if (nombreMiembro.isEmpty() || descriptor.isEmpty()) {
                throw new IllegalArgumentException(
                        "для объективо=" + objetivo + " нужны miembro и descriptor");
            }
        }
        if (linea < 1) {
            throw new IllegalArgumentException("linea < 1");
        }
    }

    /** Быстрый match по целевому классу. */
    public boolean objetivoEs(String claseInternal) {
        return this.claseInternal.equals(claseInternal);
    }
}
