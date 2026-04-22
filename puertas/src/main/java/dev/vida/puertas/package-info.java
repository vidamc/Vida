/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Access-wideners Vida — «пуэртас» («двери»).
 *
 * <p>Функционально эквивалентны Forge AT / Fabric AW: позволяют декларативно
 * ослабить доступ к членам Minecraft (сделать {@code public}, убрать
 * {@code final} с поля и т.п.), чтобы моды могли легально вызывать то, что
 * vanilla скрыла.
 *
 * <h2>Формат файла</h2>
 *
 * <p>Плоский текст, один статмент в строке. Комментарии — {@code #} до
 * конца строки. Первая не-пустая строка должна быть заголовком:
 *
 * <pre>
 * vida-puertas 1 namespace=intermedio
 * </pre>
 *
 * <p>где {@code 1} — версия формата, {@code namespace} — маппинг (intermedio
 * = Cartografía-промежуточный, crudo = obfuscated, exterior = Mojang-mapped).
 *
 * <p>Далее директивы вида:
 *
 * <pre>
 * accesible  class  net/minecraft/world/item/Item
 * accesible  method net/minecraft/world/item/Item  &lt;init&gt; (Lnet/minecraft/world/item/Item$Properties;)V
 * accesible  field  net/minecraft/world/item/Item  maxStackSize I
 * mutable    field  net/minecraft/world/item/Item  maxStackSize I
 * extensible class  net/minecraft/world/item/Item
 * </pre>
 *
 * <p>Директивы:
 * <ul>
 *   <li><b>accesible</b> — сделать {@code public} (и снять {@code private}/
 *       {@code protected}); для полей дополнительно снять {@code final},
 *       если есть;</li>
 *   <li><b>extensible</b> — снять {@code final} с класса/метода (класс
 *       становится наследуемым, метод — переопределяемым);</li>
 *   <li><b>mutable</b> — только для полей, снять {@code final}.</li>
 * </ul>
 *
 * <h2>Точка применения</h2>
 *
 * <p>{@link dev.vida.puertas.AplicadorPuertas} — ASM-трансформер, который
 * принимает набор уже распарсенных файлов и байт-код одного целевого
 * класса, а возвращает новую байтовую последовательность с применёнными
 * изменениями. Регистрируется в {@code vifada} как Escultor (hook —
 * {@code dev.vida.vifada.Transformer.registrarEscultor}).
 */
@ApiStatus.Preview("puertas")
package dev.vida.puertas;

import dev.vida.core.ApiStatus;
