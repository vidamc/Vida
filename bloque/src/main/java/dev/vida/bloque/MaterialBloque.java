/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque;

import dev.vida.core.ApiStatus;

/**
 * Классификация материала блока.
 *
 * <p>Материал задаёт базовую физику блока: горит ли он, является ли жидкостью,
 * можно ли его пушить поршнем и подобные свойства, которые vanilla 1.21.1
 * рассчитывает на основе тегов и {@code Material} (удалённый в 1.20, но по
 * сути продолжающий жить через {@code Block.Properties} и теги).
 *
 * <p>Vida предоставляет явный enum потому что:
 * <ul>
 *   <li>tag-система 1.21.1 разбросана по datapack'ам и не годится в
 *       compile-time для валидации свойств;</li>
 *   <li>моды всё равно хотят декларативно сказать «это металл» / «это
 *       растение», не собирая три отдельных тега;</li>
 *   <li>при порте на следующий мажор Minecraft внутренний mapping
 *       материала → тегов меняем централизованно, без переписывания модов.</li>
 * </ul>
 *
 * <p>Enum намеренно «плоский» — если потребуется комбинация свойств, её
 * задают флагами в {@link PropiedadesBloque}.
 */
@ApiStatus.Stable
public enum MaterialBloque {

    /** Воздух / empty-блок. Не рендерится, не коллидирует. */
    AIRE(false, false, false, false, true, true),

    /** Камень, руды, обсидиан. Не горит, не пушится если твёрдый. */
    PIEDRA(true, false, false, false, false, false),

    /** Дерево и его производные (доски, брёвна, двери). Горит. */
    MADERA(true, true, false, false, false, false),

    /** Металл (железо, золото, медь). Не горит, тяжёлый. */
    METAL(true, false, false, false, false, false),

    /** Почва, песок, гравий. Пушится, может падать (для подтипа GRAVEDAD). */
    TIERRA(true, false, false, false, false, false),

    /** Листва, трава, цветы. Горит, проницаемо для частиц. */
    PLANTA(true, true, false, false, true, true),

    /** Стекло, лёд. Прозрачное, не горит, легко разрушается. */
    CRISTAL(true, false, false, false, true, true),

    /** Шерсть, ковры, кровати. Горит очень хорошо. */
    TELA(true, true, false, false, false, false),

    /** Жидкость: вода, лава. Не коллидирует жёстко, проходима. */
    LIQUIDO(false, false, true, true, true, true),

    /** Огонь, частицы, портал. Не разрушается обычным способом. */
    EFIMERO(false, true, false, false, true, true),

    /** Базовый материал «что-то есть», без доп. семантики. */
    GENERICO(true, false, false, false, false, false);

    private final boolean solido;
    private final boolean inflamable;
    private final boolean liquido;
    private final boolean bloqueaMovimiento;
    private final boolean traversable;
    private final boolean permiteLuz;

    MaterialBloque(boolean solido, boolean inflamable, boolean liquido,
                   boolean bloqueaMovimiento, boolean traversable, boolean permiteLuz) {
        this.solido = solido;
        this.inflamable = inflamable;
        this.liquido = liquido;
        this.bloqueaMovimiento = bloqueaMovimiento;
        this.traversable = traversable;
        this.permiteLuz = permiteLuz;
    }

    /** Блок физически твёрдый (камень, дерево). */
    public boolean solido() { return solido; }

    /** Блок может загореться. */
    public boolean inflamable() { return inflamable; }

    /** Блок — жидкость (течёт, имеет level). */
    public boolean liquido() { return liquido; }

    /** Блок замедляет движение сущностей (как вода, паутина). */
    public boolean bloqueaMovimiento() { return bloqueaMovimiento; }

    /** Сущности и частицы могут проходить сквозь (растения, стекло). */
    public boolean traversable() { return traversable; }

    /** Свет проходит сквозь блок без затенения. */
    public boolean permiteLuz() { return permiteLuz; }
}
