/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Событийная шина Vida — <i>Latidos</i> («удары сердца»).
 *
 * <h2>Модель</h2>
 *
 * <p>Каждое событие описывается:
 * <ul>
 *   <li>собственным типом-значением {@code E} (обычно {@code record} или неизменяемый класс);</li>
 *   <li>единственным статическим ключом {@link dev.vida.base.latidos.Latido Latido}{@code <E>},
 *       объявленным рядом с типом события (по конвенции — поле {@code public static final Latido<E> TIPO = Latido.de(...)}).</li>
 * </ul>
 *
 * <p>Каждый {@link dev.vida.base.latidos.Oyente Oyente} регистрируется
 * на конкретный {@link dev.vida.base.latidos.Latido Latido} с заданной
 * {@link dev.vida.base.latidos.Prioridad Prioridad} и
 * {@link dev.vida.base.latidos.Fase Fase}. При {@code emitir} шина обходит
 * снимок подписчиков и вызывает {@code manejar} напрямую — без рефлексии.
 * Авто-биндер {@link dev.vida.base.latidos.LatidoRegistrador} после разрешения
 * метода по возможности использует {@link java.lang.invoke.MethodHandle}.
 *
 * <p>Для отменяемых событий параметр события должен реализовывать
 * {@link dev.vida.base.latidos.LatidoCancelable LatidoCancelable}; приёмники
 * могут вызвать {@link dev.vida.base.latidos.LatidoCancelable#cancelar() cancelar()}
 * — это не остановит доставку остальным подписчикам, но отметит
 * событие как «отменённое», и логика, породившая событие, должна это
 * учесть (идиома аналогична Minecraft-ивентам).
 *
 * <h2>Пример</h2>
 *
 * <pre>{@code
 * public record LatidoSaludo(String nombre) {
 *     public static final Latido<LatidoSaludo> TIPO =
 *             Latido.de("ejemplo:saludo", LatidoSaludo.class);
 * }
 *
 * LatidoBus bus = LatidoBus.enMemoria();
 * bus.suscribir(LatidoSaludo.TIPO, Prioridad.NORMAL,
 *         s -> System.out.println("Hola " + s.nombre()));
 *
 * bus.emitir(LatidoSaludo.TIPO, new LatidoSaludo("Mundo"));
 * }</pre>
 */
@ApiStatus.Stable
package dev.vida.base.latidos;

import dev.vida.core.ApiStatus;
