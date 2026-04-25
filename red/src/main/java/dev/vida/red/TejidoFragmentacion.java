/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Разбиение крупного логического пакета на серию {@link PaqueteClienteCargaFragmento} /
 * {@link PaqueteServidorCargaFragmento} с CRC32 целостности.
 *
 * <p>Сначала вызовите {@link #registrarFragmentos(TejidoCanal)} на канале (идемпотентно).
 */
@ApiStatus.Stable
public final class TejidoFragmentacion {

    /** Минимальный полезный размер фрагмента (байты тела), если не задан иной. */
    public static final int UMBRAL_PARTICION_DEFECTO = 48_000;

    private TejidoFragmentacion() {}

    /** Регистрирует codec версии {@code 1} для типов фрагментов (безопасно вызывать повторно). */
    public static void registrarFragmentos(TejidoCanal canal) {
        Objects.requireNonNull(canal, "canal");
        canal.registrarCliente(
                PaqueteClienteCargaFragmento.class, 1, CodecPaqueteFragmento.codecCliente());
        canal.registrarServidor(
                PaqueteServidorCargaFragmento.class, 1, CodecPaqueteFragmento.codecServidor());
    }

    /**
     * Кодирует запись и либо кладёт одну {@link TramaPaquete}, либо несколько фрагментов.
     *
     * @param maxCuerpoFragmento максимальный размер {@code fragmento} в одном фрагменте
     */
    public static <R extends Record & PaqueteCliente> Result<List<TramaPaquete>, TejidoError>
            encolarParticionadoCliente(
                    TejidoCanal canal,
                    CodecPaquete<R> codec,
                    R paquete,
                    int versionCodec,
                    int maxCuerpoFragmento) {
        return encolarParticionado(
                canal, codec, paquete, versionCodec, maxCuerpoFragmento, true);
    }

    /** Аналог {@link #encolarParticionadoCliente} для {@link PaqueteServidor}. */
    public static <R extends Record & PaqueteServidor> Result<List<TramaPaquete>, TejidoError>
            encolarParticionadoServidor(
                    TejidoCanal canal,
                    CodecPaquete<R> codec,
                    R paquete,
                    int versionCodec,
                    int maxCuerpoFragmento) {
        return encolarParticionado(
                canal, codec, paquete, versionCodec, maxCuerpoFragmento, false);
    }

    @SuppressWarnings("unchecked")
    private static <R extends Record> Result<List<TramaPaquete>, TejidoError> encolarParticionado(
            TejidoCanal canal,
            CodecPaquete<R> codec,
            R paquete,
            int versionCodec,
            int maxCuerpoFragmento,
            boolean cliente) {
        Objects.requireNonNull(canal, "canal");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(paquete, "paquete");
        if (maxCuerpoFragmento < 256) {
            throw new IllegalArgumentException("maxCuerpoFragmento < 256");
        }
        byte[] full;
        try {
            full = codec.codificar(paquete);
        } catch (RuntimeException ex) {
            return Result.err(new TejidoError.PayloadInvalido(
                    paquete.getClass().getCanonicalName(), ex.getMessage()));
        }
        if (full.length <= codec.maxCargaBytes()) {
            return canal.encolar(paquete, versionCodec).map(List::of);
        }

        int crc = CodecPaqueteFragmento.crc32(full);
        long sesion = ThreadLocalRandom.current().nextLong();
        int total = (full.length + maxCuerpoFragmento - 1) / maxCuerpoFragmento;
        List<TramaPaquete> out = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            int from = i * maxCuerpoFragmento;
            int len = Math.min(maxCuerpoFragmento, full.length - from);
            byte[] chunk = new byte[len];
            System.arraycopy(full, from, chunk, 0, len);
            Result<TramaPaquete, TejidoError> r;
            if (cliente) {
                r = canal.encolar(
                        new PaqueteClienteCargaFragmento(sesion, i, total, crc, full.length, chunk), 1);
            } else {
                r = canal.encolar(
                        new PaqueteServidorCargaFragmento(sesion, i, total, crc, full.length, chunk), 1);
            }
            if (r.isErr()) {
                return Result.err(r.unwrapErr());
            }
            out.add(r.unwrap());
        }
        return Result.ok(List.copyOf(out));
    }
}
