/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.objeto.componentes;

import dev.vida.core.ApiStatus;

/**
 * Общий маркер типизированного data-компонента.
 *
 * <p>Является sealed — так список известных vanilla-компонентов 1.21.1
 * остаётся исчерпывающим на компилятор-тайм, что помогает при обновлении
 * Vida на новый мажор Minecraft: пропущенный компонент виден как
 * compile-error, а не как silently-missing-field.
 */
@ApiStatus.Preview("objeto")
public sealed interface Componente permits
        Componente.DatosModeloPersonalizados,
        Componente.Irrompible,
        Componente.Durabilidad,
        Componente.NombrePersonalizado,
        Componente.Lore,
        Componente.MaxPila,
        Componente.Comida,
        Componente.EncantamientoOculto,
        Componente.FuegoResistente,
        Componente.BrilloEncantado,
        Componente.PerfilJugador,
        Componente.Raro,
        Componente.AtributosModificados,
        Componente.ColorTinte {

    // --------------------------------------------------------------- vanilla 1.21.1 компоненты

    /**
     * {@code minecraft:custom_model_data} — целочисленный override для
     * ресурс-пакеров.
     */
    record DatosModeloPersonalizados(int valor) implements Componente {}

    /**
     * {@code minecraft:unbreakable} — предмет не теряет durability.
     */
    record Irrompible(boolean mostrarEnTooltip) implements Componente {}

    /**
     * {@code minecraft:damage} + {@code minecraft:max_damage} в одной
     * обёртке — текущая/максимальная durability.
     */
    record Durabilidad(int dano, int maximo) implements Componente {
        public Durabilidad {
            if (maximo <= 0) {
                throw new IllegalArgumentException("maximo <= 0: " + maximo);
            }
            if (dano < 0 || dano > maximo) {
                throw new IllegalArgumentException("dano вне [0.." + maximo + "]: " + dano);
            }
        }
    }

    /**
     * {@code minecraft:custom_name} — отображаемое имя (plain-text).
     */
    record NombrePersonalizado(String texto) implements Componente {
        public NombrePersonalizado {
            if (texto == null) throw new IllegalArgumentException("texto null");
        }
    }

    /**
     * {@code minecraft:lore} — список строк описания в tooltip.
     */
    record Lore(java.util.List<String> lineas) implements Componente {
        public Lore {
            lineas = java.util.List.copyOf(lineas);
        }
    }

    /**
     * {@code minecraft:max_stack_size} — override для размера стака
     * (vanilla: от 1 до 64, для некоторых специальных — до 99).
     */
    record MaxPila(int tamanio) implements Componente {
        public MaxPila {
            if (tamanio < 1 || tamanio > 99) {
                throw new IllegalArgumentException("tamanio вне [1..99]: " + tamanio);
            }
        }
    }

    /**
     * {@code minecraft:food} — пищевые свойства.
     *
     * @param saciedad  восстанавливаемый голод (0..20)
     * @param saturacion восстанавливаемая насыщенность
     * @param puedeSiempre можно ли есть при полном голоде
     * @param tiempoComer время поедания в тиках (20 tick = 1 s)
     */
    record Comida(int saciedad, float saturacion, boolean puedeSiempre, int tiempoComer)
            implements Componente {
        public Comida {
            if (saciedad < 0 || saciedad > 20) {
                throw new IllegalArgumentException("saciedad вне [0..20]: " + saciedad);
            }
            if (saturacion < 0f) {
                throw new IllegalArgumentException("saturacion < 0: " + saturacion);
            }
            if (tiempoComer <= 0) {
                throw new IllegalArgumentException("tiempoComer <= 0: " + tiempoComer);
            }
        }
    }

    /**
     * {@code minecraft:enchantment_glint_override} с глубоко скрытым
     * тегом «не показывать список зачарований в tooltip».
     */
    record EncantamientoOculto() implements Componente {}

    /**
     * {@code minecraft:fire_resistant} — предмет не сгорает в лаве/огне.
     */
    record FuegoResistente() implements Componente {}

    /**
     * {@code minecraft:enchantment_glint_override} — визуальный блеск
     * без фактических зачарований.
     */
    record BrilloEncantado(boolean mostrar) implements Componente {}

    /**
     * {@code minecraft:profile} — профиль игрока (для голов).
     */
    record PerfilJugador(String nombre, java.util.Optional<java.util.UUID> uuid)
            implements Componente {
        public PerfilJugador {
            if (nombre == null) throw new IllegalArgumentException("nombre null");
            if (uuid == null) throw new IllegalArgumentException("uuid null (use Optional.empty())");
        }
    }

    /**
     * {@code minecraft:rarity} — override редкости.
     */
    record Raro(dev.vida.objeto.Raridad raridad) implements Componente {
        public Raro {
            if (raridad == null) throw new IllegalArgumentException("raridad null");
        }
    }

    /**
     * {@code minecraft:attribute_modifiers} — модификаторы атрибутов.
     */
    record AtributosModificados(java.util.List<ModificadorAtributo> modificadores)
            implements Componente {
        public AtributosModificados {
            modificadores = java.util.List.copyOf(modificadores);
        }

        /**
         * Одна запись модификатора атрибута.
         *
         * @param atributoId идентификатор атрибута (например, {@code "minecraft:attack_damage"})
         * @param valor       числовое значение
         * @param operacion   тип операции
         * @param slot        slot, в котором активируется модификатор
         */
        public record ModificadorAtributo(
                dev.vida.core.Identifier atributoId,
                double valor,
                Operacion operacion,
                Ranura slot) {
            public ModificadorAtributo {
                if (atributoId == null) throw new IllegalArgumentException("atributoId null");
                if (operacion == null) throw new IllegalArgumentException("operacion null");
                if (slot == null) throw new IllegalArgumentException("slot null");
            }

            /** Операции модификатора. */
            public enum Operacion { SUMAR_VALOR, SUMAR_MULTIPLICADOR_BASE, MULTIPLICAR_TOTAL }

            /** Slot, в котором активируется модификатор. */
            public enum Ranura {
                MANO_PRINCIPAL, MANO_SECUNDARIA,
                CASCO, PETO, PANTALONES, BOTAS,
                CUERPO, CUALQUIERA
            }
        }
    }

    /**
     * {@code minecraft:dyed_color} — цвет кожаной брони / цветных предметов.
     */
    record ColorTinte(int rgb) implements Componente {
        public ColorTinte {
            if ((rgb & ~0xFFFFFF) != 0) {
                throw new IllegalArgumentException("rgb вне [0..0xFFFFFF]: " + rgb);
            }
        }
    }
}
