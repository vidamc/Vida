/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.objeto;

import dev.vida.core.ApiStatus;
import dev.vida.objeto.componentes.MapaComponentes;
import java.util.Objects;
import java.util.Optional;

/**
 * Иммутабельное описание предмета.
 *
 * <p>Поля:
 * <ul>
 *   <li>{@link #tipo} — базовая категория предмета;</li>
 *   <li>{@link #maxPila} — размер стака;</li>
 *   <li>{@link #raridad} — редкость;</li>
 *   <li>{@link #herramienta} — опциональная инструментальная часть;</li>
 *   <li>{@link #componentes} — набор data-компонентов 1.21.1.</li>
 * </ul>
 *
 * <p>Собирается через fluent-билдер {@link #con()}; значения проверяются
 * в канонической форме record'а — никаких «полу-валидных» инстансов.
 */
@ApiStatus.Preview("objeto")
public record PropiedadesObjeto(
        TipoObjeto tipo,
        int maxPila,
        Raridad raridad,
        Optional<Herramienta> herramienta,
        MapaComponentes componentes) {

    public PropiedadesObjeto {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(raridad, "raridad");
        Objects.requireNonNull(herramienta, "herramienta");
        Objects.requireNonNull(componentes, "componentes");
        if (maxPila < 1 || maxPila > 99) {
            throw new IllegalArgumentException("maxPila вне [1..99]: " + maxPila);
        }
        if (tipo == TipoObjeto.HERRAMIENTA && herramienta.isEmpty()) {
            throw new IllegalArgumentException(
                    "tipo=HERRAMIENTA, но herramienta пустая");
        }
        if (tipo == TipoObjeto.HERRAMIENTA && maxPila != 1) {
            throw new IllegalArgumentException(
                    "инструмент должен иметь maxPila=1");
        }
    }

    /** Начать построение. */
    public static Constructor con() { return new Constructor(); }

    /** Быстрая проверка: это инструмент (кирка, топор, меч). */
    public boolean esHerramienta() { return herramienta.isPresent(); }

    // ==================================================================
    //                             Constructor
    // ==================================================================

    /** Fluent-билдер. */
    public static final class Constructor {

        private TipoObjeto tipo = TipoObjeto.GENERICO;
        private int maxPila = 64;
        private Raridad raridad = Raridad.COMUN;
        private Herramienta herramienta;
        private final MapaComponentes.Constructor componentes = MapaComponentes.con();

        Constructor() {}

        public Constructor tipo(TipoObjeto t) {
            this.tipo = Objects.requireNonNull(t, "tipo");
            if (t == TipoObjeto.HERRAMIENTA || t == TipoObjeto.ARMA || t == TipoObjeto.ARMADURA) {
                this.maxPila = 1;
            }
            return this;
        }

        public Constructor maxPila(int v) {
            if (v < 1 || v > 99) {
                throw new IllegalArgumentException("maxPila вне [1..99]");
            }
            this.maxPila = v;
            return this;
        }

        public Constructor raridad(Raridad r) {
            this.raridad = Objects.requireNonNull(r, "raridad");
            return this;
        }

        public Constructor herramienta(Herramienta h) {
            this.herramienta = Objects.requireNonNull(h, "herramienta");
            this.tipo = TipoObjeto.HERRAMIENTA;
            this.maxPila = 1;
            return this;
        }

        /** Добавить data-компонент. */
        public <T extends dev.vida.objeto.componentes.Componente>
                Constructor componente(dev.vida.objeto.componentes.ClaveComponente<T> clave, T valor) {
            componentes.poner(clave, valor);
            return this;
        }

        /** Заменить полный набор компонентов. */
        public Constructor componentes(MapaComponentes mapa) {
            componentes.ponerTodo(Objects.requireNonNull(mapa, "mapa"));
            return this;
        }

        public PropiedadesObjeto construir() {
            return new PropiedadesObjeto(
                    tipo, maxPila, raridad,
                    Optional.ofNullable(herramienta),
                    componentes.construir());
        }
    }
}
