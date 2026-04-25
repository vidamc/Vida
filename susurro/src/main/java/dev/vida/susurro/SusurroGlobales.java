/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.susurro;

import dev.vida.core.ApiStatus;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Один shutdown-hook на процесс: корректно ждёт завершения всех созданных Susurro.
 */
@ApiStatus.Internal
final class SusurroGlobales {

    private static final CopyOnWriteArrayList<Susurro> INSTANCIAS = new CopyOnWriteArrayList<>();
    private static volatile boolean registrado;

    private SusurroGlobales() {}

    static void registrar(Susurro sus) {
        INSTANCIAS.addIfAbsent(sus);
        synchronized (SusurroGlobales.class) {
            if (!registrado) {
                registrado = true;
                Runtime.getRuntime().addShutdownHook(Thread.ofPlatform()
                        .daemon(true)
                        .name("vida-susurro-shutdown")
                        .unstarted(() -> {
                            long techoMs = 30_000L;
                            for (Susurro s : INSTANCIAS) {
                                techoMs = Math.max(techoMs, s.politica().apagadoEsperaMsMax());
                            }
                            for (Susurro s : INSTANCIAS) {
                                s.detenerEsperando(techoMs);
                            }
                        }));
            }
        }
    }
}
