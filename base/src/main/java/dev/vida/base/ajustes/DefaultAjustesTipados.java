/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.ajustes;

import dev.vida.config.Ajustes;
import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import java.util.Objects;
import java.util.Optional;

/**
 * Базовая реализация {@link AjustesTipados}. Обёртывает
 * {@link dev.vida.config.Ajustes} и умеет извлекать типизированные значения,
 * применяя валидацию из {@link Ajuste}.
 */
@ApiStatus.Preview("base")
public final class DefaultAjustesTipados implements AjustesTipados {

    private static final Log LOG = Log.of(DefaultAjustesTipados.class);

    private final Ajustes raw;

    public DefaultAjustesTipados(Ajustes raw) {
        this.raw = Objects.requireNonNull(raw, "raw");
    }

    @Override
    public <T> T valor(Ajuste<T> ajuste) {
        Optional<T> leido = leerRaw(ajuste);
        if (leido.isEmpty()) return ajuste.defecto();
        T v = leido.get();
        Optional<String> err = ajuste.validar(v);
        if (err.isPresent()) {
            LOG.warn("Ajuste {} invalid: {} — using default", ajuste.ruta(), err.get());
            return ajuste.defecto();
        }
        return v;
    }

    @Override
    public <T> boolean establecido(Ajuste<T> ajuste) {
        return raw.contains(ajuste.ruta());
    }

    @Override
    public <T> Optional<T> leerEstricto(Ajuste<T> ajuste) {
        Optional<T> v = leerRaw(ajuste);
        if (v.isEmpty()) return Optional.empty();
        return ajuste.validar(v.get()).isPresent() ? Optional.empty() : v;
    }

    // ================================================================

    @SuppressWarnings("unchecked")
    private <T> Optional<T> leerRaw(Ajuste<T> ajuste) {
        Class<T> cls = ajuste.clase();
        String path = ajuste.ruta();
        if (cls == Integer.class) {
            return (Optional<T>) raw.findInt(path);
        }
        if (cls == Long.class) {
            return (Optional<T>) raw.findLong(path);
        }
        if (cls == Double.class) {
            return (Optional<T>) raw.findDouble(path);
        }
        if (cls == Boolean.class) {
            return (Optional<T>) raw.findBoolean(path);
        }
        if (cls == String.class) {
            return (Optional<T>) raw.findString(path);
        }
        // Произвольный тип — пытаемся сначала достать строкой, а потом
        // отдаём как-есть. В будущем здесь будет десериализация.
        return Optional.empty();
    }
}
