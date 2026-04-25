/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.platform;

import dev.vida.base.latidos.LatidoBus;
import dev.vida.core.Log;
import dev.vida.mundo.latidos.LatidosMundo;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * Эмиссия {@link LatidosMundo.Tick} для каждого {@code ServerLevel} на физическом сервере.
 */
final class ServidorLatidoMundo {

    private static final Log LOG = Log.of(ServidorLatidoMundo.class);
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    private static volatile MethodHandle servidorOverworld;
    private static volatile MethodHandle servidorGetAllLevels;

    private ServidorLatidoMundo() {}

    static void emitirSiHaySuscriptores(LatidoBus bus, Object servidor, long tick) {
        if (bus.cantidadSuscriptores(LatidosMundo.Tick.TIPO) <= 0) {
            return;
        }
        try {
            Iterable<?> niveles = nivelesDesdeServidor(servidor);
            if (niveles == null) {
                return;
            }
            for (Object nivel : niveles) {
                if (nivel == null) {
                    continue;
                }
                long ciclo = cicloDelDia(nivel);
                bus.emitir(
                        LatidosMundo.Tick.TIPO,
                        new LatidosMundo.Tick(new MundoNivelVanilla(nivel), tick, ciclo));
            }
        } catch (RuntimeException ex) {
            LOG.warn("Vida: servidor LatidosMundo.Tick ({})", ex.toString());
        } catch (Throwable ex) {
            LOG.warn("Vida: servidor LatidosMundo.Tick ({})", ex.toString());
        }
    }

    private static long cicloDelDia(Object nivel) {
        try {
            Method m = nivel.getClass().getMethod("getDayTime");
            Object v = m.invoke(nivel);
            if (v instanceof Number n) {
                return Math.floorMod(n.longValue(), 24000L);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return 0L;
    }

    private static Iterable<?> nivelesDesdeServidor(Object servidor) throws Throwable {
        Iterable<?> desdeMetodo = intentarGetAllLevels(servidor);
        if (desdeMetodo != null) {
            return desdeMetodo;
        }
        Object ow = intentarOverworld(servidor);
        if (ow != null) {
            return java.util.List.of(ow);
        }
        return null;
    }

    private static Iterable<?> intentarGetAllLevels(Object servidor) throws Throwable {
        MethodHandle mh = servidorGetAllLevels;
        if (mh == null) {
            synchronized (ServidorLatidoMundo.class) {
                if (servidorGetAllLevels == null) {
                    Method candidato = null;
                    for (String nombre : new String[] {"getAllLevels", "levels"}) {
                        try {
                            Method m = servidor.getClass().getMethod(nombre);
                            if (Iterable.class.isAssignableFrom(m.getReturnType())) {
                                candidato = m;
                                break;
                            }
                        } catch (NoSuchMethodException ignored) {
                        }
                    }
                    if (candidato != null) {
                        servidorGetAllLevels = LOOKUP.unreflect(candidato);
                    }
                }
                mh = servidorGetAllLevels;
            }
        }
        if (mh == null) {
            return null;
        }
        Object r = mh.invoke(servidor);
        return r instanceof Iterable<?> it ? it : null;
    }

    private static Object intentarOverworld(Object servidor) throws Throwable {
        MethodHandle mh = servidorOverworld;
        if (mh == null) {
            synchronized (ServidorLatidoMundo.class) {
                if (servidorOverworld == null) {
                    try {
                        Method m = servidor.getClass().getMethod("overworld");
                        servidorOverworld = LOOKUP.unreflect(m);
                    } catch (NoSuchMethodException e) {
                        servidorOverworld = null;
                    }
                }
                mh = servidorOverworld;
            }
        }
        return mh != null ? mh.invoke(servidor) : null;
    }

    static void resetForTests() {
        synchronized (ServidorLatidoMundo.class) {
            servidorOverworld = null;
            servidorGetAllLevels = null;
        }
    }
}
