/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Публичный модинг-API Vida.
 *
 * <h2>Как выглядит простой мод</h2>
 *
 * <pre>{@code
 * public final class EjemploMod implements VidaMod {
 *     @Override
 *     public void iniciar(ModContext ctx) {
 *         ctx.log().info("¡Hola, Vida! (mod {}, version {})", ctx.id(), ctx.version());
 *
 *         // Подписываемся на пульс сервера:
 *         ctx.latidos().suscribir(LatidoPulso.TIPO, Prioridad.NORMAL, pulso -> {
 *             if (pulso.tickActual() % 100 == 0) {
 *                 ctx.log().debug("tick " + pulso.tickActual());
 *             }
 *         });
 *
 *         // Регистрируем контент:
 *         Catalogo<Bloque> bloques = ctx.catalogos().obtener(Bloque.CATALOGO);
 *         bloques.registrar(CatalogoClave.de("ejemplo", "espada_sagrada"), new Bloque(...));
 *
 *         // Читаем настройки:
 *         int distancia = ctx.ajustes().valor(Ajuste.entero("render.distance", 32).min(2).max(64));
 *     }
 * }
 * }</pre>
 *
 * <h2>Дочерние пакеты</h2>
 * <ul>
 *   <li>{@link dev.vida.base.latidos} — события и шина сообщений;</li>
 *   <li>{@link dev.vida.base.catalogo} — типизированные реестры контента;</li>
 *   <li>{@link dev.vida.base.ajustes} — типизированные настройки с валидацией.</li>
 * </ul>
 *
 * <h2>Стабильность</h2>
 *
 * Модуль целиком находится в стадии {@code preview("base")} до релиза 1.0.
 * Мажорные изменения в публичных интерфейсах допускаются до объявления
 * стабильности, но всегда сопровождаются записью в {@code CHANGELOG.md} и
 * документированной процедурой миграции.
 */
@ApiStatus.Preview("base")
package dev.vida.base;

import dev.vida.core.ApiStatus;
