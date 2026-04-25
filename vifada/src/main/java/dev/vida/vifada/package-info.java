/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Vifada — байткод-трансформер Vida.
 *
 * <h2>Модель</h2>
 * <ul>
 *   <li><b>Морф</b> (от испанского «morph», форма) — класс, отмеченный
 *       {@link dev.vida.vifada.VifadaMorph @VifadaMorph} и описывающий
 *       изменения, вносимые в целевой класс.</li>
 *   <li>{@link dev.vida.vifada.VifadaInject @VifadaInject} — метод морфа
 *       вставляется в указанную точку целевого метода; порядок и место
 *       задаются {@link dev.vida.vifada.VifadaAt @VifadaAt}.</li>
 *   <li>{@link dev.vida.vifada.VifadaOverwrite @VifadaOverwrite} — тело
 *       метода морфа полностью заменяет тело целевого метода.</li>
 *   <li>{@link dev.vida.vifada.VifadaShadow @VifadaShadow} — поле или
 *       метод, существующий в целевом классе; автор морфа объявляет его в
 *       своём классе, чтобы компилятор разрешал обращения. Во время
 *       трансформации владелец обращения подменяется на целевой класс.</li>
 * </ul>
 *
 * <h2>Точки инъекции</h2>
 * В версии MVP поддерживаются {@link dev.vida.vifada.InjectionPoint#HEAD} и
 * {@link dev.vida.vifada.InjectionPoint#RETURN}. Остальные точки из
 * перечисления зарезервированы; их реализация — предмет отдельной фазы.
 *
 * <h2>CallbackInfo</h2>
 * Каждый {@code @VifadaInject}-метод принимает последним параметром
 * {@link dev.vida.vifada.CallbackInfo}, через который можно прервать
 * выполнение целевого метода ({@code ci.cancel()}).
 *
 * <h2>Пример</h2>
 * <pre>{@code
 * @VifadaMorph(target = "com.mojang.Clock")
 * public abstract class ClockMorph {
 *     @VifadaShadow private int ticks;
 *
 *     @VifadaInject(method = "tick()V", at = @VifadaAt(InjectionPoint.HEAD))
 *     public void onTick(CallbackInfo ci) {
 *         if (ticks > 1000) ci.cancel();
 *     }
 * }
 * }</pre>
 *
 * <h2>Применение</h2>
 * Высокий уровень — {@link dev.vida.vifada.Transformer#transform} принимает
 * байты целевого класса и коллекцию {@link dev.vida.vifada.MorphSource} и
 * возвращает трансформированные байты либо структурированную ошибку
 * {@link dev.vida.vifada.VifadaError}.
 */
@ApiStatus.Stable
package dev.vida.vifada;

import dev.vida.core.ApiStatus;
