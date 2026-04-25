/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.CRC32;

/**
 * Собирает полный payload из {@link PaqueteClienteCargaFragmento} или
 * {@link PaqueteServidorCargaFragmento}.
 */
@ApiStatus.Stable
public final class TejidoEnsambladorFragmentos {

    private final Map<Long, Sesion> sesiones = new HashMap<>();
    private final int maxMensajeBytes;

    public TejidoEnsambladorFragmentos(int maxMensajeBytes) {
        if (maxMensajeBytes < 1024) {
            throw new IllegalArgumentException("maxMensajeBytes < 1024");
        }
        this.maxMensajeBytes = maxMensajeBytes;
    }

    /**
     * Принимает декодированный фрагмент; при завершении возвращает полные байты.
     */
    public Result<Optional<byte[]>, TejidoError> aceptarCliente(PaqueteClienteCargaFragmento f) {
        return aceptar(f.sesion(), f.indice(), f.total(), f.crc32(), f.longitudTotal(), f.fragmento());
    }

    /** @see #aceptarCliente */
    public Result<Optional<byte[]>, TejidoError> aceptarServidor(PaqueteServidorCargaFragmento f) {
        return aceptar(f.sesion(), f.indice(), f.total(), f.crc32(), f.longitudTotal(), f.fragmento());
    }

    private Result<Optional<byte[]>, TejidoError> aceptar(
            long sesion, int indice, int total, int crc32, int longitudTotal, byte[] fragmento) {
        if (longitudTotal > maxMensajeBytes) {
            return Result.err(
                    new TejidoError.CargaDemasiadoGrande("fragment-session", longitudTotal, maxMensajeBytes));
        }
        synchronized (sesiones) {
            Sesion s = sesiones.computeIfAbsent(sesion, ignored -> new Sesion(total, longitudTotal, crc32));
            if (s.total != total || s.longitudTotal != longitudTotal || s.crc32 != crc32) {
                sesiones.remove(sesion);
                return Result.err(new TejidoError.PayloadInvalido(
                        "fragment-session", "conflicting session metadata"));
            }
            if (s.partes[indice] != null) {
                return Result.err(new TejidoError.PayloadInvalido("fragment-session", "duplicate index"));
            }
            s.partes[indice] = fragmento.clone();
            s.recibidos++;
            if (s.recibidos < total) {
                return Result.ok(Optional.empty());
            }
            sesiones.remove(sesion);
            byte[] full = new byte[longitudTotal];
            int off = 0;
            for (byte[] p : s.partes) {
                System.arraycopy(p, 0, full, off, p.length);
                off += p.length;
            }
            CRC32 c = new CRC32();
            c.update(full);
            int actual = (int) c.getValue();
            if (actual != crc32) {
                return Result.err(new TejidoError.PayloadInvalido(
                        "fragment-session", "crc mismatch expected=" + crc32 + " actual=" + actual));
            }
            return Result.ok(Optional.of(full));
        }
    }

    private static final class Sesion {
        final int total;
        final int longitudTotal;
        final int crc32;
        final byte[][] partes;
        int recibidos;

        Sesion(int total, int longitudTotal, int crc32) {
            this.total = total;
            this.longitudTotal = longitudTotal;
            this.crc32 = crc32;
            this.partes = new byte[total][];
        }
    }
}
