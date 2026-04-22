/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.puertas;

import dev.vida.core.ApiStatus;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Результат парсинга одного .ptr-файла: заголовок + список директив.
 *
 * <p>Экземпляр иммутабельный. Директивы хранятся и в виде полного списка,
 * и в виде индекса по {@code claseInternal} для быстрого отбора перед
 * применением.
 */
@ApiStatus.Preview("puertas")
public final class PuertaArchivo {

    private final String nombreOrigen;
    private final int version;
    private final Namespace namespace;
    private final List<PuertaDirectiva> directivas;
    private final Map<String, List<PuertaDirectiva>> porClase;

    PuertaArchivo(String nombreOrigen, int version, Namespace namespace,
                  List<PuertaDirectiva> directivas) {
        this.nombreOrigen = Objects.requireNonNull(nombreOrigen, "nombreOrigen");
        this.version = version;
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.directivas = List.copyOf(directivas);
        this.porClase = this.directivas.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.groupingBy(PuertaDirectiva::claseInternal,
                                Collectors.toUnmodifiableList()),
                        Collections::unmodifiableMap));
    }

    public String nombreOrigen()                { return nombreOrigen; }
    public int version()                        { return version; }
    public Namespace namespace()                { return namespace; }
    public List<PuertaDirectiva> directivas()   { return directivas; }

    /** Директивы, относящиеся к заданному internal-имени класса (может быть пусто). */
    public List<PuertaDirectiva> paraClase(String claseInternal) {
        Objects.requireNonNull(claseInternal, "claseInternal");
        return porClase.getOrDefault(claseInternal, List.of());
    }

    /** Все затронутые internal-имена классов. */
    public java.util.Set<String> clases() {
        return porClase.keySet();
    }

    /**
     * Сливает несколько файлов в один — удобно для AplicadorPuertas, который
     * работает с единым набором директив. namespace'ы должны совпадать;
     * при несовпадении бросается {@link IllegalArgumentException}.
     */
    public static PuertaArchivo combinar(String nombreOrigen, Iterable<PuertaArchivo> partes) {
        Objects.requireNonNull(nombreOrigen, "nombreOrigen");
        Objects.requireNonNull(partes, "partes");
        Namespace ns = null;
        int ver = -1;
        List<PuertaDirectiva> todas = new ArrayList<>();
        for (PuertaArchivo p : partes) {
            if (ns == null) { ns = p.namespace; ver = p.version; }
            else if (ns != p.namespace) {
                throw new IllegalArgumentException(
                        "несовпадение namespace: " + ns + " ↔ " + p.namespace + " в " + p.nombreOrigen);
            }
            todas.addAll(p.directivas);
        }
        if (ns == null) {
            ns = Namespace.INTERMEDIO;
            ver = 1;
        }
        return new PuertaArchivo(nombreOrigen, ver, ns, todas);
    }
}
