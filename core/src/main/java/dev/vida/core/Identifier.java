/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.core;

import java.util.Objects;
import java.util.Optional;

/**
 * Namespaced-идентификатор вида {@code namespace:path}.
 *
 * <p>Используется везде, где нужен уникальный ключ в контенте Vida: блоки,
 * предметы, регистрации в {@link ApiStatus.Stable Catalogo}, события Latidos,
 * файлы Fuente и т.д.
 *
 * <h2>Грамматика</h2>
 * <ul>
 *   <li><b>namespace</b>: {@code [a-z0-9_.-]+}</li>
 *   <li><b>path</b>:      {@code [a-z0-9_./-]+}</li>
 * </ul>
 *
 * <p>Разделитель — один символ {@code ':'}. В path дополнительно разрешён
 * {@code '/'} для иерархических ключей (например {@code minecraft:block/stone}).
 *
 * <h2>Примеры</h2>
 * <pre>{@code
 *   Identifier.of("ejemplo", "espada_sagrada")
 *   Identifier.parse("minecraft:block/stone")
 * }</pre>
 *
 * <p>Реализовано как {@code record} для неизменяемости и дешёвого equals/hashCode.
 */
@ApiStatus.Stable
public record Identifier(String namespace, String path) implements Comparable<Identifier> {

    /** Стандартный разделитель. */
    public static final char SEPARATOR = ':';

    /** Максимальная общая длина {@code namespace:path}. Защита от DoS при разборе манифестов. */
    public static final int MAX_LENGTH = 256;

    /** Конструктор-валидатор. */
    public Identifier {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (namespace.isEmpty()) {
            throw new IllegalArgumentException("identifier namespace is empty");
        }
        if (path.isEmpty()) {
            throw new IllegalArgumentException("identifier path is empty");
        }
        if (namespace.length() + 1 + path.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "identifier too long (" + (namespace.length() + 1 + path.length())
                            + " > " + MAX_LENGTH + "): " + namespace + ":" + path);
        }
        for (int i = 0, n = namespace.length(); i < n; i++) {
            if (!isValidNamespaceChar(namespace.charAt(i))) {
                throw new IllegalArgumentException(
                        "invalid character in namespace '" + namespace
                                + "' at index " + i + ": '" + namespace.charAt(i) + "'");
            }
        }
        for (int i = 0, n = path.length(); i < n; i++) {
            if (!isValidPathChar(path.charAt(i))) {
                throw new IllegalArgumentException(
                        "invalid character in path '" + path
                                + "' at index " + i + ": '" + path.charAt(i) + "'");
            }
        }
    }

    /** Фабрика. */
    public static Identifier of(String namespace, String path) {
        return new Identifier(namespace, path);
    }

    /**
     * Строгий парсер. Требует один {@code ':'} и валидные части.
     *
     * @throws IllegalArgumentException если строка не соответствует грамматике
     */
    public static Identifier parse(String combined) {
        Objects.requireNonNull(combined, "combined");
        int sep = combined.indexOf(SEPARATOR);
        if (sep < 0) {
            throw new IllegalArgumentException("missing ':' in identifier: " + combined);
        }
        if (combined.indexOf(SEPARATOR, sep + 1) >= 0) {
            throw new IllegalArgumentException("multiple ':' in identifier: " + combined);
        }
        return new Identifier(combined.substring(0, sep), combined.substring(sep + 1));
    }

    /** Нестрогая версия парсера. */
    public static Optional<Identifier> tryParse(String combined) {
        if (combined == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(parse(combined));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /**
     * Парсер с неявным namespace. Если {@code combined} не содержит {@code ':'},
     * возвращается идентификатор {@code defaultNamespace:combined}.
     */
    public static Identifier parseWithDefault(String combined, String defaultNamespace) {
        Objects.requireNonNull(combined, "combined");
        Objects.requireNonNull(defaultNamespace, "defaultNamespace");
        int sep = combined.indexOf(SEPARATOR);
        if (sep < 0) {
            return new Identifier(defaultNamespace, combined);
        }
        return parse(combined);
    }

    /** Проверка без бросания исключений. */
    public static boolean isValid(String combined) {
        return tryParse(combined).isPresent();
    }

    /** Сравнение: сначала по namespace, затем по path. */
    @Override
    public int compareTo(Identifier other) {
        int ns = this.namespace.compareTo(other.namespace);
        return ns != 0 ? ns : this.path.compareTo(other.path);
    }

    @Override
    public String toString() {
        return namespace + SEPARATOR + path;
    }

    // ------------------------------------------------------------------ utils

    private static boolean isValidNamespaceChar(char c) {
        return (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9')
                || c == '_' || c == '-' || c == '.';
    }

    private static boolean isValidPathChar(char c) {
        return isValidNamespaceChar(c) || c == '/';
    }
}
