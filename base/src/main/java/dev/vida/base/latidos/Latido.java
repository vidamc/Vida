/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;

/**
 * Типизированный ключ события для {@link LatidoBus}.
 *
 * <p>Один экземпляр {@link Latido} — один «канал» событий с фиксированным
 * типом значения {@code E}. По конвенции объявляется как
 * {@code public static final Latido<Event> TIPO = Latido.de(...)}
 * рядом с самим классом события.
 *
 * <p>Два {@link Latido} считаются равными только если совпадают и
 * {@link #id() id}, и {@link #claseEvento() класс события} — это защита от
 * дурака, если кто-то случайно зарегистрирует несколько подписчиков на
 * одинаковый id с разным типом (классическая ошибка в мод-лоадерах, где
 * приходится ловить {@code ClassCastException}).
 *
 * @param <E> тип значения события
 */
@ApiStatus.Preview("base")
public final class Latido<E> {

    private final Identifier id;
    private final Class<E> claseEvento;
    private final boolean cancelable;

    private Latido(Identifier id, Class<E> claseEvento) {
        this.id = Objects.requireNonNull(id, "id");
        this.claseEvento = Objects.requireNonNull(claseEvento, "claseEvento");
        this.cancelable = LatidoCancelable.class.isAssignableFrom(claseEvento);
    }

    public Identifier id()              { return id; }
    public Class<E> claseEvento()       { return claseEvento; }
    public boolean esCancelable()       { return cancelable; }

    /** Фабрика с id в виде строки {@code "namespace:path"}. */
    public static <E> Latido<E> de(String combined, Class<E> claseEvento) {
        return new Latido<>(Identifier.parse(combined), claseEvento);
    }

    /** Фабрика с готовым {@link Identifier}. */
    public static <E> Latido<E> de(Identifier id, Class<E> claseEvento) {
        return new Latido<>(id, claseEvento);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Latido<?> other)) return false;
        return id.equals(other.id) && claseEvento.equals(other.claseEvento);
    }

    @Override
    public int hashCode() {
        return id.hashCode() * 31 + claseEvento.hashCode();
    }

    @Override
    public String toString() {
        return "Latido(" + id + " : " + claseEvento.getSimpleName() + ")";
    }
}
