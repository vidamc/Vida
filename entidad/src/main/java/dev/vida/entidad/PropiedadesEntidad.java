/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.entidad;

import dev.vida.core.ApiStatus;
import dev.vida.entidad.componentes.ClaveComponenteEntidad;
import dev.vida.entidad.componentes.ComponenteEntidad;
import dev.vida.entidad.componentes.MapaComponentesEntidad;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Иммутабельное описание свойств entity-type.
 */
@ApiStatus.Stable
public record PropiedadesEntidad(
        double masa,
        Hitbox hitbox,
        Set<GrupoIa> gruposIa,
        MapaComponentesEntidad componentes) {

    public PropiedadesEntidad {
        if (!Double.isFinite(masa) || masa <= 0d) {
            throw new IllegalArgumentException("masa debe ser finita y > 0: " + masa);
        }
        Objects.requireNonNull(hitbox, "hitbox");
        Objects.requireNonNull(gruposIa, "gruposIa");
        Objects.requireNonNull(componentes, "componentes");
        gruposIa = Set.copyOf(gruposIa);
    }

    public static Constructor con() {
        return new Constructor();
    }

    public boolean tieneIa() {
        return !gruposIa.isEmpty();
    }

    public double volumenHitbox() {
        return hitbox.volumen();
    }

    /** Трёхмерный box сущности. */
    @ApiStatus.Stable
    public record Hitbox(double ancho, double alto, double profundidad) {
        public Hitbox {
            validarDimension("ancho", ancho);
            validarDimension("alto", alto);
            validarDimension("profundidad", profundidad);
        }

        private static void validarDimension(String nombre, double valor) {
            if (!Double.isFinite(valor) || valor <= 0d) {
                throw new IllegalArgumentException(nombre + " debe ser finito y > 0: " + valor);
            }
        }

        public double volumen() {
            return ancho * alto * profundidad;
        }
    }

    /** Семейство поведения AI. */
    @ApiStatus.Stable
    public enum GrupoIa {
        TERRESTRE,
        VOLADOR,
        NADADOR,
        PASIVO,
        HERBIVORO,
        HOSTIL,
        DOMESTICABLE,
        COMERCIANTE,
        JEFE
    }

    /** Fluent-билдер. */
    public static final class Constructor {
        private double masa = 1.0d;
        private Hitbox hitbox = new Hitbox(0.6d, 1.8d, 0.6d);
        private final EnumSet<GrupoIa> gruposIa = EnumSet.noneOf(GrupoIa.class);
        private final MapaComponentesEntidad.Constructor componentes = MapaComponentesEntidad.con();

        Constructor() {}

        public Constructor masa(double masa) {
            if (!Double.isFinite(masa) || masa <= 0d) {
                throw new IllegalArgumentException("masa debe ser finita y > 0");
            }
            this.masa = masa;
            return this;
        }

        public Constructor hitbox(double ancho, double alto, double profundidad) {
            this.hitbox = new Hitbox(ancho, alto, profundidad);
            return this;
        }

        public Constructor hitbox(Hitbox hitbox) {
            this.hitbox = Objects.requireNonNull(hitbox, "hitbox");
            return this;
        }

        public Constructor grupoIa(GrupoIa grupoIa) {
            gruposIa.add(Objects.requireNonNull(grupoIa, "grupoIa"));
            return this;
        }

        public Constructor gruposIa(GrupoIa... gruposIa) {
            Objects.requireNonNull(gruposIa, "gruposIa");
            for (GrupoIa grupoIa : gruposIa) {
                this.gruposIa.add(Objects.requireNonNull(grupoIa, "grupoIa"));
            }
            return this;
        }

        public <T extends ComponenteEntidad> Constructor componente(ClaveComponenteEntidad<T> clave, T valor) {
            componentes.poner(clave, valor);
            return this;
        }

        public Constructor componentes(MapaComponentesEntidad mapa) {
            componentes.ponerTodo(Objects.requireNonNull(mapa, "mapa"));
            return this;
        }

        public PropiedadesEntidad construir() {
            return new PropiedadesEntidad(masa, hitbox, gruposIa, componentes.construir());
        }
    }
}
