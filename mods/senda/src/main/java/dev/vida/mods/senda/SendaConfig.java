/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.senda;

import dev.vida.base.ajustes.Ajuste;
import dev.vida.base.ajustes.AjustesTipados;
import dev.vida.core.ApiStatus;

/**
 * Конфигурация мода Senda, прочитанная из {@code senda.toml}.
 *
 * <h2>Параметры</h2>
 * <ul>
 *   <li>{@code maxPuntosPorDimension} — максимальное число активных путевых точек
 *       на измерение (1–1000; по умолчанию 50).</li>
 *   <li>{@code dimensionInicial} — измерение, выбираемое по умолчанию при открытии
 *       HUD Senda. Допустимые значения: {@code overworld}, {@code nether}, {@code end}.</li>
 * </ul>
 *
 * @param maxPuntosPorDimension максимальное число точек на измерение
 * @param dimensionInicial      измерение по умолчанию
 */
@ApiStatus.Preview("senda")
public record SendaConfig(int maxPuntosPorDimension, String dimensionInicial) {

    // ---------------------------------------------------------------- Ajuste descriptors

    static final Ajuste<Integer> AJUSTE_MAX_PUNTOS =
            Ajuste.entero("maxPuntosPorDimension", 50)
                    .descripcion("Максимальное число путевых точек на измерение (1–1000).")
                    .min(1)
                    .max(1000)
                    .build();

    static final Ajuste<String> AJUSTE_DIMENSION =
            Ajuste.cadena("dimensionInicial", "overworld")
                    .descripcion("Измерение по умолчанию: overworld | nether | end.")
                    .verificar(v -> {
                        String lower = v.trim().toLowerCase(java.util.Locale.ROOT);
                        return lower.equals("overworld") || lower.equals("nether") || lower.equals("end")
                                ? java.util.Optional.empty()
                                : java.util.Optional.of(
                                        "Неизвестное измерение \"" + v
                                        + "\"; допустимые: overworld, nether, end.");
                    })
                    .build();

    /** Значения по умолчанию. */
    public static SendaConfig defecto() {
        return new SendaConfig(50, "overworld");
    }

    /**
     * Читает конфигурацию из типизированного {@link AjustesTipados}.
     * При невалидных значениях используются умолчания.
     *
     * @param tipados источник настроек
     * @return распарсенная конфигурация
     */
    public static SendaConfig desde(AjustesTipados tipados) {
        int max = tipados.valor(AJUSTE_MAX_PUNTOS);
        String dim = tipados.valor(AJUSTE_DIMENSION);
        return new SendaConfig(max, dim.trim().toLowerCase(java.util.Locale.ROOT));
    }
}
