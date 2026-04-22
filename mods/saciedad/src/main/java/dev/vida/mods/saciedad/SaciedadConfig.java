/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.saciedad;

import dev.vida.base.ajustes.Ajuste;
import dev.vida.base.ajustes.AjustesTipados;
import dev.vida.core.ApiStatus;
import java.util.Locale;

/**
 * Конфигурация мода Saciedad, прочитанная из {@code saciedad.toml}.
 *
 * <h2>Параметры</h2>
 * <ul>
 *   <li>{@code color} — цвет шкалы в hex ARGB (по умолчанию {@code FFFFA500} — янтарный).</li>
 *   <li>{@code mostrarSiempre} — показывать шкалу даже при нулевом насыщении.</li>
 *   <li>{@code posicion} — положение шкалы: {@code arriba}, {@code abajo}, {@code encima}.</li>
 * </ul>
 *
 * @param color          цвет заливки в ARGB
 * @param mostrarSiempre показывать при нулевом насыщении
 * @param posicion       позиция относительно шкалы голода
 */
@ApiStatus.Preview("saciedad")
public record SaciedadConfig(int color, boolean mostrarSiempre, Posicion posicion) {

    /** Положение шкалы насыщения относительно шкалы голода. */
    public enum Posicion {
        /** Над шкалой голода. */
        ARRIBA,
        /** Под шкалой голода. */
        ABAJO,
        /** Поверх шкалы голода. */
        ENCIMA;

        public static Posicion desde(String raw) {
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "arriba" -> ARRIBA;
                case "encima" -> ENCIMA;
                default -> ABAJO;
            };
        }
    }

    // ---------------------------------------------------------------- Ajuste descriptors

    static final Ajuste<String> AJUSTE_COLOR =
            Ajuste.cadena("color", "FFFFA500")
                    .descripcion("Цвет шкалы насыщения в hex ARGB (например: FFFFA500).")
                    .build();

    static final Ajuste<Boolean> AJUSTE_MOSTRAR_SIEMPRE =
            Ajuste.logico("mostrarSiempre", false)
                    .descripcion("Показывать шкалу даже при нулевом насыщении.")
                    .build();

    static final Ajuste<String> AJUSTE_POSICION =
            Ajuste.cadena("posicion", "abajo")
                    .descripcion("Позиция шкалы: arriba | abajo | encima.")
                    .verificar(v -> {
                        String lower = v.trim().toLowerCase(Locale.ROOT);
                        return lower.equals("arriba") || lower.equals("abajo") || lower.equals("encima")
                                ? java.util.Optional.empty()
                                : java.util.Optional.of(
                                        "Неизвестная позиция \"" + v + "\"; допустимые: arriba, abajo, encima.");
                    })
                    .build();

    /** Значения по умолчанию (янтарный цвет, под шкалой, скрыт при нуле). */
    public static SaciedadConfig defecto() {
        return new SaciedadConfig(0xFFFFA500, false, Posicion.ABAJO);
    }

    /**
     * Читает конфигурацию из типизированного {@link AjustesTipados}.
     *
     * <p>Никогда не бросает: при невалидных значениях используются умолчания.
     *
     * @param tipados источник настроек
     * @return распарсенная конфигурация
     */
    public static SaciedadConfig desde(AjustesTipados tipados) {
        String colorStr = tipados.valor(AJUSTE_COLOR);
        int color = parseColor(colorStr);

        boolean mostrar = tipados.valor(AJUSTE_MOSTRAR_SIEMPRE);

        String posStr = tipados.valor(AJUSTE_POSICION);
        Posicion posicion = Posicion.desde(posStr);

        return new SaciedadConfig(color, mostrar, posicion);
    }

    // ---------------------------------------------------------------- helpers

    /**
     * Парсит hex-строку ARGB в int. При ошибке возвращает янтарный цвет.
     * Принимает 6-символьный RGB (alpha = FF) и 8-символьный ARGB.
     */
    static int parseColor(String hex) {
        String trimmed = hex.trim().replace("#", "");
        try {
            if (trimmed.length() == 6) {
                return (int) (0xFF000000L | Long.parseLong(trimmed, 16));
            }
            return (int) Long.parseLong(trimmed, 16);
        } catch (IllegalArgumentException ignore) {
            return 0xFFFFA500;
        }
    }
}
