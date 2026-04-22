/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.manifest.json;

/**
 * Ошибка парсинга Vida-JSON. Содержит {@code line}/{@code column} для диагностики.
 */
public class JsonException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int line;
    private final int column;

    public JsonException(String message, int line, int column) {
        super(formatMessage(message, line, column));
        this.line = line;
        this.column = column;
    }

    public JsonException(String message, int line, int column, Throwable cause) {
        super(formatMessage(message, line, column), cause);
        this.line = line;
        this.column = column;
    }

    public int line() { return line; }
    public int column() { return column; }

    private static String formatMessage(String msg, int line, int col) {
        return msg + " (at line " + line + ", column " + col + ")";
    }
}
