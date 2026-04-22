/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.ajustes;

import dev.vida.core.ApiStatus;

/**
 * Типизированный доступ к настройкам мода.
 *
 * <p>Поверх {@link dev.vida.config.Ajustes} (raw TOML-like key/value) даёт
 * единообразный путь: {@code tipados.valor(ajuste)} — значение, валидированное
 * по {@link Ajuste#validador()}; при невалидном значении используется
 * {@link Ajuste#defecto()}.
 */
@ApiStatus.Preview("base")
public interface AjustesTipados {

    /** Читает значение с применением валидации. Никогда не бросает. */
    <T> T valor(Ajuste<T> ajuste);

    /** {@code true}, если по пути {@link Ajuste#ruta()} есть установленное значение. */
    <T> boolean establecido(Ajuste<T> ajuste);

    /**
     * Пытается прочитать значение без фоллбэка. Возвращает {@link java.util.Optional#empty()},
     * если значения нет или оно не того типа; при ошибке валидации тоже
     * возвращает пусто.
     */
    <T> java.util.Optional<T> leerEstricto(Ajuste<T> ajuste);

    /** Фабрика: обёртывает существующий {@link dev.vida.config.Ajustes}. */
    static AjustesTipados sobre(dev.vida.config.Ajustes raw) {
        return new DefaultAjustesTipados(raw);
    }
}
