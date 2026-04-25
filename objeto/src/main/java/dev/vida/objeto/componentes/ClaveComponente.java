/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.objeto.componentes;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;

/**
 * Типизированный ключ data-компонента.
 *
 * <p>Связывает текстовый идентификатор (например {@code minecraft:food}) с
 * типом компонента {@link T extends Componente}. Используется как ключ в
 * {@link MapaComponentes} и одновременно соответствует vanilla
 * {@code DataComponentType<T>} (мост поверх — в будущем {@code vida-mundo}).
 *
 * @param <T> фактический тип компонента
 */
@ApiStatus.Stable
public final class ClaveComponente<T extends Componente> {

    private final Identifier id;
    private final Class<T> tipo;

    private ClaveComponente(Identifier id, Class<T> tipo) {
        this.id = Objects.requireNonNull(id, "id");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
    }

    public Identifier id() { return id; }
    public Class<T> tipo() { return tipo; }

    public static <T extends Componente> ClaveComponente<T> de(Identifier id, Class<T> tipo) {
        return new ClaveComponente<>(id, tipo);
    }

    public static <T extends Componente> ClaveComponente<T> de(String id, Class<T> tipo) {
        return new ClaveComponente<>(Identifier.parse(id), tipo);
    }

    // ------------------------------------------------------------------
    //  Vanilla-ключи 1.21.1, соответствующие записям в Componente.
    // ------------------------------------------------------------------

    public static final ClaveComponente<Componente.DatosModeloPersonalizados> DATOS_MODELO =
            de("minecraft:custom_model_data", Componente.DatosModeloPersonalizados.class);

    public static final ClaveComponente<Componente.Irrompible> IRROMPIBLE =
            de("minecraft:unbreakable", Componente.Irrompible.class);

    public static final ClaveComponente<Componente.Durabilidad> DURABILIDAD =
            de("minecraft:damage", Componente.Durabilidad.class);

    public static final ClaveComponente<Componente.NombrePersonalizado> NOMBRE =
            de("minecraft:custom_name", Componente.NombrePersonalizado.class);

    public static final ClaveComponente<Componente.Lore> LORE =
            de("minecraft:lore", Componente.Lore.class);

    public static final ClaveComponente<Componente.MaxPila> MAX_PILA =
            de("minecraft:max_stack_size", Componente.MaxPila.class);

    public static final ClaveComponente<Componente.Comida> COMIDA =
            de("minecraft:food", Componente.Comida.class);

    public static final ClaveComponente<Componente.EncantamientoOculto> ENCANT_OCULTO =
            de("minecraft:hide_tooltip", Componente.EncantamientoOculto.class);

    public static final ClaveComponente<Componente.FuegoResistente> FUEGO_RESISTENTE =
            de("minecraft:fire_resistant", Componente.FuegoResistente.class);

    public static final ClaveComponente<Componente.BrilloEncantado> BRILLO =
            de("minecraft:enchantment_glint_override", Componente.BrilloEncantado.class);

    public static final ClaveComponente<Componente.PerfilJugador> PERFIL =
            de("minecraft:profile", Componente.PerfilJugador.class);

    public static final ClaveComponente<Componente.Raro> RARIDAD =
            de("minecraft:rarity", Componente.Raro.class);

    public static final ClaveComponente<Componente.AtributosModificados> ATRIBUTOS =
            de("minecraft:attribute_modifiers", Componente.AtributosModificados.class);

    public static final ClaveComponente<Componente.ColorTinte> COLOR =
            de("minecraft:dyed_color", Componente.ColorTinte.class);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClaveComponente<?> other)) return false;
        return id.equals(other.id) && tipo.equals(other.tipo);
    }

    @Override
    public int hashCode() {
        return id.hashCode() * 31 + tipo.hashCode();
    }

    @Override
    public String toString() {
        return "ClaveComponente(" + id + " : " + tipo.getSimpleName() + ")";
    }
}
