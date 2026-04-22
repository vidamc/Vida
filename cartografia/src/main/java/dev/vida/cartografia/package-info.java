/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Cartografía — модуль мэппингов имён Vida.
 *
 * <p>Публичные типы:
 * <ul>
 *   <li>{@link dev.vida.cartografia.Namespace} — value-тип пространства имён;</li>
 *   <li>{@link dev.vida.cartografia.MappingTree} — иммутабельное дерево
 *       классов/полей/методов в нескольких namespace;</li>
 *   <li>{@link dev.vida.cartografia.ClassMapping},
 *       {@link dev.vida.cartografia.FieldMapping},
 *       {@link dev.vida.cartografia.MethodMapping} — элементы дерева;</li>
 *   <li>{@link dev.vida.cartografia.MappingFormat} — известные форматы (PROGUARD/CTG);</li>
 *   <li>{@link dev.vida.cartografia.MappingError} — структурированные ошибки чтения/доступа.</li>
 * </ul>
 *
 * <p>Ввод/вывод — в подпакете {@code io}: Proguard-мэппинги Mojang и
 * собственный компактный формат {@code .ctg}. Интеграция с ASM — в {@code asm}.
 */
package dev.vida.cartografia;
