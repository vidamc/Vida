/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.config;

import dev.vida.core.ApiStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Нейтральное представление данных конфига Vida.
 *
 * <p>Модель намеренно независима от TOML: одно и то же дерево можно получить
 * при парсинге TOML, JSON5 или runtime-оверрайдов. Это позволяет merge-логике
 * работать единообразно, а пользователю — не зависеть от формата.
 *
 * <h2>Типы узлов</h2>
 * <ul>
 *   <li>{@link Table} — ассоциативная таблица строк → узлов. Порядок вставки
 *       сохраняется (на основе {@link LinkedHashMap}) для детерминированной
 *       сериализации и сообщений об ошибках.</li>
 *   <li>{@link Array} — упорядоченный список узлов.</li>
 *   <li>{@link Str}/{@link Int}/{@link Dbl}/{@link Bool} — примитивные скаляры.</li>
 * </ul>
 *
 * <p>Все узлы иммутабельны после создания. Для точечных правок корневой
 * таблицы используйте {@link Table#toBuilder()}.
 */
@ApiStatus.Stable
public sealed interface ConfigNode
        permits ConfigNode.Table, ConfigNode.Array,
                ConfigNode.Str, ConfigNode.Int, ConfigNode.Dbl, ConfigNode.Bool {

    /** Короткое имя типа для сообщений об ошибках. */
    String typeName();

    // =========================================================== factories

    static Table emptyTable() { return Table.EMPTY; }

    static Table table(Map<String, ConfigNode> entries) {
        return entries.isEmpty() ? Table.EMPTY : new Table(new LinkedHashMap<>(entries));
    }

    static Array array(List<ConfigNode> items) {
        return items.isEmpty() ? Array.EMPTY : new Array(List.copyOf(items));
    }

    static Str str(String v) { return new Str(Objects.requireNonNull(v, "v")); }
    static Int integer(long v) { return new Int(v); }
    static Dbl real(double v) { return new Dbl(v); }
    static Bool bool(boolean v) { return v ? Bool.TRUE : Bool.FALSE; }

    // ================================================================ types

    /** Таблица. */
    final class Table implements ConfigNode {
        /** Канонический пустой экземпляр. */
        public static final Table EMPTY = new Table(new LinkedHashMap<>());

        private final LinkedHashMap<String, ConfigNode> entries;

        Table(LinkedHashMap<String, ConfigNode> entries) {
            this.entries = entries;
        }

        /** Неизменяемый вид таблицы. */
        public Map<String, ConfigNode> entries() {
            return Collections.unmodifiableMap(entries);
        }

        public boolean isEmpty() { return entries.isEmpty(); }
        public int size() { return entries.size(); }
        public boolean has(String key) { return entries.containsKey(key); }

        /** Узел по одному ключу; {@code null} если нет. */
        public ConfigNode get(String key) {
            return entries.get(Objects.requireNonNull(key, "key"));
        }

        /** Доступ по dotted-path: {@code a.b.c}. Возвращает {@code null} если любого звена нет. */
        public ConfigNode find(String path) {
            Objects.requireNonNull(path, "path");
            if (path.isEmpty()) return this;
            ConfigNode cur = this;
            int start = 0;
            int len = path.length();
            for (int i = 0; i <= len; i++) {
                if (i == len || path.charAt(i) == '.') {
                    if (!(cur instanceof Table t)) return null;
                    String segment = path.substring(start, i);
                    if (segment.isEmpty()) return null;
                    cur = t.entries.get(segment);
                    if (cur == null) return null;
                    start = i + 1;
                }
            }
            return cur;
        }

        public Builder toBuilder() {
            Builder b = new Builder();
            b.entries.putAll(this.entries);
            return b;
        }

        @Override public String typeName() { return "table"; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Table other)) return false;
            return entries.equals(other.entries);
        }

        @Override public int hashCode() { return entries.hashCode(); }

        @Override
        public String toString() {
            return "Table" + entries;
        }

        // ---------------------- Builder ----------------------

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private final LinkedHashMap<String, ConfigNode> entries = new LinkedHashMap<>();

            public Builder put(String key, ConfigNode value) {
                entries.put(Objects.requireNonNull(key, "key"),
                        Objects.requireNonNull(value, "value"));
                return this;
            }

            public Builder putString(String key, String v) { return put(key, str(v)); }
            public Builder putInt(String key, long v)       { return put(key, integer(v)); }
            public Builder putDouble(String key, double v)  { return put(key, real(v)); }
            public Builder putBool(String key, boolean v)   { return put(key, bool(v)); }

            public Table build() {
                return entries.isEmpty() ? EMPTY : new Table(new LinkedHashMap<>(entries));
            }
        }
    }

    /** Массив. */
    final class Array implements ConfigNode {
        /** Канонический пустой экземпляр. */
        public static final Array EMPTY = new Array(List.of());

        private final List<ConfigNode> items;

        Array(List<ConfigNode> items) { this.items = items; }

        public List<ConfigNode> items() { return items; }
        public int size() { return items.size(); }
        public boolean isEmpty() { return items.isEmpty(); }
        public ConfigNode get(int index) { return items.get(index); }

        /** Неизменяемый билдер. */
        public static Builder builder() { return new Builder(); }

        @Override public String typeName() { return "array"; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Array other)) return false;
            return items.equals(other.items);
        }

        @Override public int hashCode() { return items.hashCode(); }

        @Override public String toString() { return items.toString(); }

        public static final class Builder {
            private final List<ConfigNode> buf = new ArrayList<>();
            public Builder add(ConfigNode v) { buf.add(Objects.requireNonNull(v, "v")); return this; }
            public Builder addString(String v) { return add(str(v)); }
            public Builder addInt(long v)      { return add(integer(v)); }
            public Builder addDouble(double v) { return add(real(v)); }
            public Builder addBool(boolean v)  { return add(bool(v)); }
            public Array build() { return buf.isEmpty() ? EMPTY : new Array(List.copyOf(buf)); }
        }
    }

    /** Строка. */
    record Str(String value) implements ConfigNode {
        public Str { Objects.requireNonNull(value, "value"); }
        @Override public String typeName() { return "string"; }
    }

    /** Знаковое 64-битное целое. */
    record Int(long value) implements ConfigNode {
        @Override public String typeName() { return "integer"; }
    }

    /** 64-битное число с плавающей точкой. */
    record Dbl(double value) implements ConfigNode {
        @Override public String typeName() { return "float"; }
    }

    /** Логическое значение. */
    record Bool(boolean value) implements ConfigNode {
        static final Bool TRUE  = new Bool(true);
        static final Bool FALSE = new Bool(false);
        @Override public String typeName() { return "boolean"; }
    }

    // ----------------------------------------------------------- convenience

    /**
     * Универсальный конвертер: для {@link Table} — вызывается {@link Table#toBuilder}.
     * Для остальных типов — {@link UnsupportedOperationException}.
     *
     * <p>Используется внутренне в merge-логике.
     */
    default Table.Builder toBuilder() {
        throw new UnsupportedOperationException("toBuilder is only supported for Table, got " + typeName());
    }
}
