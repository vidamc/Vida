/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.config.internal;

import dev.vida.config.ConfigNode;
import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlPosition;
import org.tomlj.TomlTable;

/**
 * Конвертация результата разбора TOML (через {@code tomlj}) в нейтральный
 * {@link ConfigNode}.
 *
 * <p>Это единственное место во всём {@code :config}, где импортируются классы
 * {@code org.tomlj.*}. В дальнейшем, если мы решим заменить tomlj на собственный
 * TOML-парсер или поддержать JSON5/YAML — понадобится поменять только этот файл.
 */
@ApiStatus.Internal
public final class TomlBridge {

    private TomlBridge() {}

    /**
     * Парсит строку TOML в {@link ConfigNode.Table}. При ошибках возвращает
     * {@link ParseFailure} с координатами первой ошибки.
     */
    public static Result<ConfigNode.Table, ParseFailure> parse(String sourceName, String toml) {
        TomlParseResult r = Toml.parse(toml);
        if (r.hasErrors()) {
            TomlParseError first = r.errors().get(0);
            TomlPosition pos = first.position();
            int line = pos != null ? pos.line() : 0;
            int col  = pos != null ? pos.column() : 0;
            return Result.err(new ParseFailure(sourceName, line, col, first.getMessage()));
        }
        return Result.ok(toTable(r));
    }

    /** Рекурсивно конвертирует TomlTable. */
    public static ConfigNode.Table toTable(TomlTable t) {
        if (t.isEmpty()) return ConfigNode.Table.EMPTY;
        ConfigNode.Table.Builder b = ConfigNode.Table.builder();
        for (String key : t.keySet()) {
            // tomlj отдаёт ключи «как есть» (уже неквотированные / раскодированные).
            b.put(key, convert(t.get(key)));
        }
        return b.build();
    }

    /** Рекурсивно конвертирует TomlArray. */
    public static ConfigNode.Array toArray(TomlArray a) {
        if (a.isEmpty()) return ConfigNode.Array.EMPTY;
        ConfigNode.Array.Builder b = ConfigNode.Array.builder();
        for (int i = 0, n = a.size(); i < n; i++) {
            b.add(convert(a.get(i)));
        }
        return b.build();
    }

    /**
     * Конвертирует произвольное значение, отдаваемое tomlj.
     * Поддерживает: TomlTable, TomlArray, String, Long, Double, Boolean.
     * Даты/времена из TOML не поддерживаются в этой версии: если встретятся —
     * конвертируются в строку {@code toString()}.
     */
    public static ConfigNode convert(Object value) {
        if (value == null) {
            // TOML не допускает null, но tomlj для отсутствующих возвращает null.
            return ConfigNode.str("");
        }
        if (value instanceof TomlTable t) return toTable(t);
        if (value instanceof TomlArray a) return toArray(a);
        if (value instanceof String s)    return ConfigNode.str(s);
        if (value instanceof Long l)      return ConfigNode.integer(l);
        if (value instanceof Integer i)   return ConfigNode.integer(i.longValue());
        if (value instanceof Double d)    return ConfigNode.real(d);
        if (value instanceof Float f)     return ConfigNode.real(f.doubleValue());
        if (value instanceof Boolean b)   return ConfigNode.bool(b);
        // Fallback для дат, OffsetDateTime и прочего — строковая форма.
        return ConfigNode.str(value.toString());
    }

    /** Лёгкая ошибка парсинга TOML, чтобы не тащить {@code AjustesError} в этот пакет. */
    public record ParseFailure(String source, int line, int column, String detail) {}
}
