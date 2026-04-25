/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */

/**
 * Типизированные настройки Vida — <i>Ajustes</i>.
 *
 * <p>Этот пакет — высокоуровневая надстройка над
 * {@link dev.vida.config.Ajustes} из {@code :config}: там «сырые»
 * key-value значения, здесь — описания настроек с типом, умолчанием,
 * диапазоном и валидацией.
 *
 * <pre>{@code
 *   public final class Ajustes {
 *       public static final Ajuste<Integer> DISTANCIA =
 *               Ajuste.entero("render.distance", 32).min(2).max(64).build();
 *   }
 *
 *   int d = ctx.ajustes().valor(Ajustes.DISTANCIA);
 * }</pre>
 *
 * <p>Прочтённая настройка валидируется по всем указанным правилам; при
 * нарушении — используется {@link dev.vida.base.ajustes.Ajuste#defecto()}
 * и в лог уходит предупреждение.
 */
@ApiStatus.Stable
package dev.vida.base.ajustes;

import dev.vida.core.ApiStatus;
