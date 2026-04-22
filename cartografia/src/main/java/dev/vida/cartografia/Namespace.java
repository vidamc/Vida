/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Именованное пространство мэппингов.
 *
 * <p>Vida-конвенция предусматривает минимум три известных namespace:
 * <ul>
 *   <li>{@link #OBF} — обфусцированные runtime-имена (то, что реально лежит в jar игры);</li>
 *   <li>{@link #MOJMAP} — mojmap (человекочитаемые имена Mojang, закрытый lgpl-совместимый dataset);</li>
 *   <li>{@link #NAMED} — финальные имена, которые видят модеры (обычно mojmap, при желании — community-aliases).</li>
 * </ul>
 *
 * <p>Остальные namespace ({@code intermediate}, {@code legacy}, …) создаются пользователем
 * через {@link #of(String)} — ограничений на символы нет, но по соглашению
 * используются нижний регистр, цифры и {@code '-'}/{@code '_'}.
 */
@ApiStatus.Stable
public record Namespace(String name) implements Comparable<Namespace> {

    /** Обфусцированные имена из production jar-а. */
    public static final Namespace OBF = new Namespace("obf");

    /** Mojang-овские читаемые имена. */
    public static final Namespace MOJMAP = new Namespace("mojmap");

    /** Финальное пространство имён, которое видят авторы модов. */
    public static final Namespace NAMED = new Namespace("named");

    public Namespace {
        Objects.requireNonNull(name, "name");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("namespace name is empty");
        }
        if (name.length() > 64) {
            throw new IllegalArgumentException("namespace name too long: " + name);
        }
    }

    /** Удобная фабрика. */
    public static Namespace of(String name) {
        return new Namespace(name);
    }

    @Override
    public int compareTo(Namespace o) {
        return this.name.compareTo(o.name);
    }

    @Override
    public String toString() {
        return name;
    }
}
