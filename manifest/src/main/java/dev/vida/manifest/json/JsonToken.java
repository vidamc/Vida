/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.manifest.json;

/**
 * Типы токенов Vida-JSON pull-парсера.
 */
public enum JsonToken {
    /** Начало объекта (открывающая фигурная скобка). */
    BEGIN_OBJECT,
    /** Конец объекта (закрывающая фигурная скобка). */
    END_OBJECT,
    /** Начало массива (открывающая квадратная скобка). */
    BEGIN_ARRAY,
    /** Конец массива (закрывающая квадратная скобка). */
    END_ARRAY,
    /** Имя поля объекта — JSON-строка без раскавычивания. */
    NAME,
    /** Строковое значение. */
    STRING,
    /** Число — {@code int}/{@code long}/{@code double}; итоговый тип выбирается читателем. */
    NUMBER,
    /** {@code true} или {@code false}. */
    BOOLEAN,
    /** {@code null}. */
    NULL,
    /** Конец документа. */
    EOF
}
