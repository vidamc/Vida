/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.objeto.componentes;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Типизированная неизменяемая карта data-компонентов.
 *
 * <p>Хранит компоненты по {@link ClaveComponente}. Чтение возвращает
 * типизированный {@link Optional}, запись — через билдер {@link Constructor},
 * результат всегда иммутабельный.
 *
 * <p>Поведение совпадает с vanilla {@code DataComponentMap}: один ключ —
 * один компонент; переопределение заменяет предыдущее значение.
 */
@ApiStatus.Preview("objeto")
public final class MapaComponentes {

    private static final MapaComponentes VACIO = new MapaComponentes(Map.of());

    private final Map<Identifier, Componente> porId;

    private MapaComponentes(Map<Identifier, Componente> porId) {
        this.porId = porId;
    }

    /** Пустая карта. */
    public static MapaComponentes vacio() { return VACIO; }

    /** Начать построение. */
    public static Constructor con() { return new Constructor(); }

    // ------------------------------------------------------------------ query

    /** Количество компонентов. */
    public int tamanio() { return porId.size(); }

    /** Есть ли хоть один компонент. */
    public boolean esVacio() { return porId.isEmpty(); }

    /** Есть ли компонент с данным ключом. */
    public boolean contiene(ClaveComponente<?> clave) {
        Objects.requireNonNull(clave, "clave");
        Componente c = porId.get(clave.id());
        return c != null && clave.tipo().isInstance(c);
    }

    /** Получить типизированный компонент. */
    @SuppressWarnings("unchecked")
    public <T extends Componente> Optional<T> obtener(ClaveComponente<T> clave) {
        Objects.requireNonNull(clave, "clave");
        Componente c = porId.get(clave.id());
        if (c == null) return Optional.empty();
        if (!clave.tipo().isInstance(c)) return Optional.empty();
        return Optional.of((T) c);
    }

    /** Все значения в порядке вставки. */
    public Collection<Componente> valores() {
        return porId.values();
    }

    /** Все идентификаторы известных компонентов. */
    public Collection<Identifier> ids() {
        return porId.keySet();
    }

    /**
     * Сливает текущую карту с {@code otra} — значения {@code otra}
     * побеждают. Возвращает новый иммутабельный объект.
     */
    public MapaComponentes fusionar(MapaComponentes otra) {
        Objects.requireNonNull(otra, "otra");
        if (otra.esVacio()) return this;
        Map<Identifier, Componente> copia = new LinkedHashMap<>(this.porId);
        copia.putAll(otra.porId);
        return new MapaComponentes(Map.copyOf(copia));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MapaComponentes other)) return false;
        return porId.equals(other.porId);
    }

    @Override
    public int hashCode() { return porId.hashCode(); }

    @Override
    public String toString() { return "MapaComponentes" + porId; }

    // ==================================================================
    //                             Constructor
    // ==================================================================

    /** Fluent-билдер. */
    public static final class Constructor {

        private final Map<Identifier, Componente> buf = new LinkedHashMap<>();

        Constructor() {}

        /** Кладёт компонент. */
        public <T extends Componente> Constructor poner(ClaveComponente<T> clave, T valor) {
            Objects.requireNonNull(clave, "clave");
            Objects.requireNonNull(valor, "valor");
            if (!clave.tipo().isInstance(valor)) {
                throw new ClassCastException(
                        "значение " + valor.getClass().getName()
                                + " не является " + clave.tipo().getName());
            }
            buf.put(clave.id(), valor);
            return this;
        }

        /** Удаляет компонент по ключу. */
        public Constructor quitar(ClaveComponente<?> clave) {
            Objects.requireNonNull(clave, "clave");
            buf.remove(clave.id());
            return this;
        }

        /** Накатывает все записи {@code mapa}. */
        public Constructor ponerTodo(MapaComponentes mapa) {
            Objects.requireNonNull(mapa, "mapa");
            buf.putAll(mapa.porId);
            return this;
        }

        /** Строит иммутабельную карту. */
        public MapaComponentes construir() {
            if (buf.isEmpty()) return vacio();
            return new MapaComponentes(Map.copyOf(buf));
        }
    }
}
