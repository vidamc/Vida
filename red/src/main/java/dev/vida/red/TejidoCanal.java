/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Памятный канал Tejido с versioned codecs и back-pressure.
 */
@ApiStatus.Preview("red")
public final class TejidoCanal {

    private final int maxCola;
    private final EnumMap<DireccionPaquete, Map<String, NavigableMap<Integer, CodecPaquete<? extends Record>>>>
            codecs = new EnumMap<>(DireccionPaquete.class);
    private final ArrayDeque<TramaPaquete> pendientes = new ArrayDeque<>();

    private long encolados;
    private long rechazadosBackPressure;

    public TejidoCanal(int maxCola) {
        if (maxCola < 1) {
            throw new IllegalArgumentException("maxCola < 1");
        }
        this.maxCola = maxCola;
        codecs.put(DireccionPaquete.CLIENTE_A_SERVIDOR, new LinkedHashMap<>());
        codecs.put(DireccionPaquete.SERVIDOR_A_CLIENTE, new LinkedHashMap<>());
    }

    public <T extends Record & PaqueteCliente> void registrarRecordCliente(Class<T> tipo, int versionCodec) {
        registrarCliente(tipo, versionCodec, CodificadorRegistros.para(tipo));
    }

    public <T extends Record & PaqueteServidor> void registrarRecordServidor(Class<T> tipo, int versionCodec) {
        registrarServidor(tipo, versionCodec, CodificadorRegistros.para(tipo));
    }

    public <T extends Record & PaqueteCliente> void registrarCliente(
            Class<T> tipo,
            int versionCodec,
            CodecPaquete<T> codec) {
        registrar(DireccionPaquete.CLIENTE_A_SERVIDOR, tipo, versionCodec, codec);
    }

    public <T extends Record & PaqueteServidor> void registrarServidor(
            Class<T> tipo,
            int versionCodec,
            CodecPaquete<T> codec) {
        registrar(DireccionPaquete.SERVIDOR_A_CLIENTE, tipo, versionCodec, codec);
    }

    public synchronized Result<TramaPaquete, TejidoError> encolar(Record paquete, int versionCodec) {
        Objects.requireNonNull(paquete, "paquete");
        if (versionCodec < 1) {
            throw new IllegalArgumentException("versionCodec < 1");
        }
        DireccionPaquete direccion = direccionDe(paquete.getClass());
        String tipoCanonical = paquete.getClass().getCanonicalName();
        NavigableMap<Integer, CodecPaquete<? extends Record>> porVersion =
                codecs.get(direccion).get(tipoCanonical);
        if (porVersion == null) {
            return Result.err(new TejidoError.TipoNoRegistrado(tipoCanonical, direccion));
        }
        @SuppressWarnings("unchecked")
        CodecPaquete<Record> codec = (CodecPaquete<Record>) porVersion.get(versionCodec);
        if (codec == null) {
            return Result.err(new TejidoError.VersionNoSoportada(tipoCanonical, versionCodec));
        }
        if (pendientes.size() >= maxCola) {
            rechazadosBackPressure++;
            return Result.err(new TejidoError.BackPressure(maxCola));
        }

        try {
            byte[] payload = codec.codificar(paquete);
            TramaPaquete trama = new TramaPaquete(direccion, tipoCanonical, versionCodec, payload);
            pendientes.add(trama);
            encolados++;
            return Result.ok(trama);
        } catch (IllegalArgumentException ex) {
            return Result.err(new TejidoError.PayloadInvalido(tipoCanonical, ex.getMessage()));
        }
    }

    public synchronized Result<Record, TejidoError> decodificar(TramaPaquete trama) {
        Objects.requireNonNull(trama, "trama");
        Map<String, NavigableMap<Integer, CodecPaquete<? extends Record>>> porTipo =
                codecs.get(trama.direccion());
        NavigableMap<Integer, CodecPaquete<? extends Record>> porVersion =
                porTipo.get(trama.tipoCanonical());
        if (porVersion == null) {
            return Result.err(new TejidoError.TipoNoRegistrado(trama.tipoCanonical(), trama.direccion()));
        }
        Map.Entry<Integer, CodecPaquete<? extends Record>> entry = porVersion.floorEntry(trama.versionCodec());
        if (entry == null) {
            return Result.err(new TejidoError.VersionNoSoportada(trama.tipoCanonical(), trama.versionCodec()));
        }

        try {
            return Result.ok(entry.getValue().decodificar(trama.payload()));
        } catch (IllegalArgumentException ex) {
            return Result.err(new TejidoError.PayloadInvalido(trama.tipoCanonical(), ex.getMessage()));
        }
    }

    public synchronized List<TramaPaquete> drenarPendientes() {
        List<TramaPaquete> out = new ArrayList<>(pendientes);
        pendientes.clear();
        return List.copyOf(out);
    }

    public synchronized int pendientes() {
        return pendientes.size();
    }

    public int maxCola() {
        return maxCola;
    }

    public synchronized Estadisticas estadisticas() {
        return new Estadisticas(encolados, rechazadosBackPressure, pendientes.size(), maxCola);
    }

    private static DireccionPaquete direccionDe(Class<?> tipo) {
        boolean cliente = PaqueteCliente.class.isAssignableFrom(tipo);
        boolean servidor = PaqueteServidor.class.isAssignableFrom(tipo);
        if (cliente == servidor) {
            throw new IllegalArgumentException(
                    "el paquete debe implementar exactamente un marcador: " + tipo.getName());
        }
        return cliente ? DireccionPaquete.CLIENTE_A_SERVIDOR : DireccionPaquete.SERVIDOR_A_CLIENTE;
    }

    private <T extends Record> void registrar(
            DireccionPaquete direccion,
            Class<T> tipo,
            int versionCodec,
            CodecPaquete<T> codec) {
        Objects.requireNonNull(direccion, "direccion");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(codec, "codec");
        if (versionCodec < 1) {
            throw new IllegalArgumentException("versionCodec < 1");
        }
        if (!tipo.isRecord()) {
            throw new IllegalArgumentException("tipo no es record: " + tipo.getName());
        }
        String tipoCanonical = Objects.requireNonNull(tipo.getCanonicalName(), "tipo canonical");
        synchronized (this) {
            NavigableMap<Integer, CodecPaquete<? extends Record>> byVersion =
                    codecs.get(direccion).computeIfAbsent(tipoCanonical, ignored -> new TreeMap<>());
            byVersion.put(versionCodec, codec);
        }
    }

    /**
     * Snapshot метрик back-pressure и буфера.
     */
    @ApiStatus.Preview("red")
    public record Estadisticas(
            long encolados,
            long rechazadosBackPressure,
            int pendientes,
            int maxCola) {}
}
