/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.config;

import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Иммутабельный снимок конфигурации Vida.
 *
 * <p>Обёртка поверх одного {@link ConfigNode.Table} с удобными типизированными
 * геттерами. Доступ по dotted-path: {@code "renderer.distance"},
 * {@code "network.endpoints.server"}.
 *
 * <h2>Два семейства геттеров</h2>
 * <ul>
 *   <li><b>strict</b> ({@link #getInt(String)}) — кидают исключение, если ключа
 *       нет или тип не совпадает. Удобны при фиксированных схемах.</li>
 *   <li><b>optional</b> ({@link #findInt(String)}) — возвращают {@link Optional}
 *       и игнорируют отсутствие. Удобны для мягких оверрайдов.</li>
 *   <li><b>with default</b> ({@link #getInt(String, int)}) — возвращают
 *       дефолт при отсутствии/несовпадении типа.</li>
 * </ul>
 *
 * <p>Класс не содержит собственной validation-логики; для схем и диапазонов —
 * см. {@code vida-base/Ajustes API} (Phase 9+). Здесь — minimal viable access.
 */
@ApiStatus.Stable
public final class Ajustes {

    private final ConfigNode.Table root;

    Ajustes(ConfigNode.Table root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    /** Пустая конфигурация. */
    public static Ajustes empty() {
        return new Ajustes(ConfigNode.Table.EMPTY);
    }

    /** Оборачивает существующее дерево узлов. */
    public static Ajustes of(ConfigNode.Table root) {
        return new Ajustes(root);
    }

    /** Корневая таблица (для расширенного доступа/сериализации). */
    public ConfigNode.Table root() { return root; }

    /** {@code true}, если по пути есть ненулевое значение. */
    public boolean contains(String path) {
        return root.find(path) != null;
    }

    /** Сырой узел по пути; {@link Optional#empty()} если нет. */
    public Optional<ConfigNode> node(String path) {
        return Optional.ofNullable(root.find(path));
    }

    // ==================================================== optional getters

    public Optional<String> findString(String path) {
        ConfigNode n = root.find(path);
        return n instanceof ConfigNode.Str s ? Optional.of(s.value()) : Optional.empty();
    }

    public Optional<Long> findLong(String path) {
        ConfigNode n = root.find(path);
        return n instanceof ConfigNode.Int i ? Optional.of(i.value()) : Optional.empty();
    }

    public Optional<Double> findDouble(String path) {
        ConfigNode n = root.find(path);
        if (n instanceof ConfigNode.Dbl d) return Optional.of(d.value());
        // Разрешаем «повышение» int → double для удобства.
        if (n instanceof ConfigNode.Int i) return Optional.of((double) i.value());
        return Optional.empty();
    }

    public Optional<Boolean> findBoolean(String path) {
        ConfigNode n = root.find(path);
        return n instanceof ConfigNode.Bool b ? Optional.of(b.value()) : Optional.empty();
    }

    /** Дочерняя таблица как {@link Ajustes}. */
    public Optional<Ajustes> findTable(String path) {
        ConfigNode n = root.find(path);
        return n instanceof ConfigNode.Table t ? Optional.of(new Ajustes(t)) : Optional.empty();
    }

    /** Список строк по пути. Элементы нестрокового типа отбрасываются. */
    public Optional<List<String>> findStringList(String path) {
        ConfigNode n = root.find(path);
        if (!(n instanceof ConfigNode.Array a)) return Optional.empty();
        List<String> out = new java.util.ArrayList<>(a.size());
        for (ConfigNode item : a.items()) {
            if (item instanceof ConfigNode.Str s) out.add(s.value());
        }
        return Optional.of(List.copyOf(out));
    }

    // ====================================================== default getters

    public String getString(String path, String def) { return findString(path).orElse(def); }
    public long   getLong(String path, long def)     { return findLong(path).orElse(def); }
    public int    getInt(String path, int def)       { return findInt(path).orElse(def); }
    public double getDouble(String path, double def) { return findDouble(path).orElse(def); }
    public boolean getBoolean(String path, boolean def) { return findBoolean(path).orElse(def); }

    public Optional<Integer> findInt(String path) {
        return findLong(path).flatMap(v -> {
            if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) return Optional.empty();
            return Optional.of(v.intValue());
        });
    }

    // ==================================================== strict getters

    public String getString(String path) {
        return require(path, findString(path), "string");
    }

    public long getLong(String path) {
        return require(path, findLong(path), "integer");
    }

    public int getInt(String path) {
        long v = getLong(path);
        if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
            throw new AjustesAccessException(
                    new AjustesError.OutOfRange(path, v + " overflows int"));
        }
        return (int) v;
    }

    public double getDouble(String path) {
        return require(path, findDouble(path), "float");
    }

    public boolean getBoolean(String path) {
        return require(path, findBoolean(path), "boolean");
    }

    public Ajustes getTable(String path) {
        return require(path, findTable(path), "table");
    }

    // ==================================================== Result-style

    public Result<String, AjustesError> tryString(String path) {
        return resolve(path, "string", findString(path));
    }

    public Result<Long, AjustesError> tryLong(String path) {
        return resolve(path, "integer", findLong(path));
    }

    public Result<Integer, AjustesError> tryInt(String path) {
        return tryLong(path).flatMap(v -> {
            if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) {
                return Result.err(new AjustesError.OutOfRange(path, v + " overflows int"));
            }
            return Result.ok(v.intValue());
        });
    }

    public Result<Double, AjustesError> tryDouble(String path) {
        return resolve(path, "float", findDouble(path));
    }

    public Result<Boolean, AjustesError> tryBoolean(String path) {
        return resolve(path, "boolean", findBoolean(path));
    }

    public Result<Ajustes, AjustesError> tryTable(String path) {
        return resolve(path, "table", findTable(path));
    }

    // ----------------------------------------------------------- helpers

    private <T> T require(String path, Optional<T> v, String expected) {
        if (v.isPresent()) return v.get();
        throw new AjustesAccessException(classifyMissing(path, expected));
    }

    private <T> Result<T, AjustesError> resolve(String path, String expected, Optional<T> v) {
        if (v.isPresent()) return Result.ok(v.get());
        return Result.err(classifyMissing(path, expected));
    }

    private AjustesError classifyMissing(String path, String expected) {
        ConfigNode actual = root.find(path);
        if (actual == null) return new AjustesError.Missing(path);
        return new AjustesError.TypeMismatch(path, expected, actual.typeName());
    }

    @Override
    public String toString() {
        return "Ajustes(" + root.size() + " keys)";
    }
}
