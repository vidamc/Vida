/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import dev.vida.base.latidos.eventos.LatidoPulso;
import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import dev.vida.susurro.Etiqueta;
import dev.vida.susurro.HiloPrincipal;
import dev.vida.susurro.Susurro;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Биндер аннотаций: сканирует методы объекта, помеченные
 * {@link EjecutorLatido @EjecutorLatido}, и подписывает их на
 * соответствующие каналы {@link LatidoBus}.
 *
 * <p>После разрешения метода рантайм-вызов идёт через
 * {@link java.lang.invoke.MethodHandle}; при недоступности {@code unreflect}
 * (границы модулей) — запасной путь {@code Method.invoke}.
 *
 * <h2>Алгоритм привязки</h2>
 * <ol>
 *   <li>Для каждого метода с {@code @EjecutorLatido} проверяется сигнатура:
 *       ровно один параметр типа {@code E}.</li>
 *   <li>На классе {@code E} ищется поле {@code static final Latido<E>}
 *       (по конвенции — {@code TIPO}). Если не найдено, поиск продолжается
 *       в классе-владельце метода.</li>
 *   <li>Создаётся {@link Ejecutor} по {@link EjecutorLatido#kind()}.</li>
 *   <li>Метод подписывается на шину с {@link EjecutorLatido#prioridadBus()}
 *       и {@link EjecutorLatido#fase()}.</li>
 * </ol>
 *
 * <h2>Пример</h2>
 * <pre>{@code
 * public class MiMod {
 *     @EjecutorLatido(kind = Kind.HILO_PRINCIPAL)
 *     public void alArrancar(LatidoArranque ev) {
 *         System.out.println("Vida started: " + ev.vidaVersion());
 *     }
 * }
 *
 * var suscripciones = LatidoRegistrador.registrarEnObjeto(bus, new MiMod(), susurro, hp);
 * }</pre>
 *
 * @see EjecutorLatido
 */
@ApiStatus.Stable
public final class LatidoRegistrador {

    private static final Log LOG = Log.of(LatidoRegistrador.class);

    private LatidoRegistrador() {}

    /**
     * Сканирует {@code instance} и подписывает все методы с
     * {@code @EjecutorLatido} на шину.
     *
     * <p>Для {@link EjecutorLatido.Kind#SUSURRO} и
     * {@link EjecutorLatido.Kind#HILO_PRINCIPAL} необходимы соответствующие
     * аргументы. Если метод требует исполнитель, который не передан,
     * бросается {@link LatidoRegistradorError.EjecutorFaltante}.
     *
     * @param bus            целевая шина событий
     * @param instance       объект, методы которого сканируются
     * @param susurro        пул Susurro (может быть {@code null}, если
     *                       нет SUSURRO-методов)
     * @param hiloPrincipal  главный поток (может быть {@code null}, если
     *                       нет HILO_PRINCIPAL-методов)
     * @return список подписок для возможной отмены
     * @throws LatidoRegistradorError при ошибке валидации
     */
    public static List<Suscripcion> registrarEnObjeto(LatidoBus bus,
                                                       Object instance,
                                                       Susurro susurro,
                                                       HiloPrincipal hiloPrincipal) {
        Objects.requireNonNull(bus, "bus");
        Objects.requireNonNull(instance, "instance");

        List<Suscripcion> resultado = new ArrayList<>();
        Class<?> cls = instance.getClass();

        for (Method m : cls.getDeclaredMethods()) {
            EjecutorLatido ejecutorLatido = m.getAnnotation(EjecutorLatido.class);
            LatidosProfundos latidosProfundos = m.getAnnotation(LatidosProfundos.class);
            OyenteDeTick oyenteDeTick = m.getAnnotation(OyenteDeTick.class);
            if (latidosProfundos != null && ejecutorLatido == null && oyenteDeTick == null) {
                throw new LatidoRegistradorError.MarcadorLatidosProfundosSinEjecutor(m);
            }
            if (ejecutorLatido == null && oyenteDeTick == null) continue;
            if (ejecutorLatido != null && oyenteDeTick != null) {
                throw new LatidoRegistradorError.AnotacionesConflictivas(m);
            }

            Suscripcion s = ejecutorLatido != null
                    ? vincularMetodo(bus, instance, m, ejecutorLatido, susurro, hiloPrincipal)
                    : vincularTick(bus, instance, m, oyenteDeTick, susurro, hiloPrincipal);
            resultado.add(s);
        }

        LOG.debug("registrarEnObjeto: {} suscripciones de {}",
                resultado.size(), cls.getName());
        return List.copyOf(resultado);
    }

    /**
     * Variante sin {@link Susurro}/{@link HiloPrincipal}: solo admite
     * métodos con {@link EjecutorLatido.Kind#SINCRONO}.
     */
    public static List<Suscripcion> registrarEnObjeto(LatidoBus bus, Object instance) {
        return registrarEnObjeto(bus, instance, null, null);
    }

    // ================================================================

    @SuppressWarnings("unchecked")
    private static <E> Suscripcion vincularMetodo(LatidoBus bus,
                                                   Object instance,
                                                   Method metodo,
                                                   EjecutorLatido anotacion,
                                                   Susurro susurro,
                                                   HiloPrincipal hp) {
        Class<?>[] params = metodo.getParameterTypes();
        if (params.length != 1) {
            throw new LatidoRegistradorError.FirmaInvalida(metodo, params.length);
        }

        Class<E> tipoEvento = (Class<E>) params[0];
        Latido<E> latido = resolverLatido(metodo, tipoEvento, instance.getClass());
        Ejecutor ejecutor = crearEjecutor(metodo, anotacion, susurro, hp);

        if (!latido.claseEvento().isAssignableFrom(tipoEvento)) {
            throw new LatidoRegistradorError.TipoIncompatible(
                    metodo, latido.claseEvento(), tipoEvento);
        }

        metodo.setAccessible(true);

        Prioridad prioridadBus = anotacion.prioridadBus().aLatidos();
        Fase fase = anotacion.fase().aLatidos();
        MethodHandle manija = manijaInstancia(metodo, instance);
        Oyente<E> oyente = manija != null
                ? evento -> invocarManija(manija, evento)
                : evento -> invocarMetodo(instance, metodo, evento);

        LOG.debug("  {} → {} [{}] ejecutor={} prioridad={} fase={}",
                metodo.getName(), latido, tipoEvento.getSimpleName(),
                anotacion.kind(), prioridadBus, fase);

        return bus.suscribir(latido, prioridadBus, fase, ejecutor, oyente);
    }

    private static Suscripcion vincularTick(LatidoBus bus,
                                            Object instance,
                                            Method metodo,
                                            OyenteDeTick anotacion,
                                            Susurro susurro,
                                            HiloPrincipal hp) {
        Class<?>[] params = metodo.getParameterTypes();
        if (params.length != 1) {
            throw new LatidoRegistradorError.FirmaInvalida(metodo, "@OyenteDeTick", params.length);
        }
        if (params[0] != LatidoPulso.class) {
            throw new LatidoRegistradorError.TipoIncompatible(
                    metodo, "@OyenteDeTick", LatidoPulso.class, params[0]);
        }
        if (anotacion.tps() < 1 || anotacion.tps() > 20) {
            throw new LatidoRegistradorError.TpsInvalido(metodo, anotacion.tps());
        }

        metodo.setAccessible(true);

        Prioridad prioridadBus = anotacion.prioridadBus().aLatidos();
        Fase fase = anotacion.fase().aLatidos();
        Ejecutor ejecutor = crearEjecutor(
                metodo,
                anotacion.kind(),
                anotacion.prioridad(),
                anotacion.etiqueta(),
                susurro,
                hp,
                "@OyenteDeTick");
        MethodHandle manijaTick = manijaInstancia(metodo, instance);
        Oyente<LatidoPulso> oyente = manijaTick != null
                ? evento -> {
                    if (evento.profundidad() != 0) {
                        return;
                    }
                    if (!debeEjecutarTick(evento.tickActual(), anotacion.tps())) {
                        return;
                    }
                    invocarManija(manijaTick, evento);
                }
                : evento -> {
                    if (evento.profundidad() != 0) {
                        return;
                    }
                    if (!debeEjecutarTick(evento.tickActual(), anotacion.tps())) {
                        return;
                    }
                    invocarMetodo(instance, metodo, evento);
                };

        LOG.debug("  {} → {} [LatidoPulso] ejecutor={} prioridad={} fase={} tps={}",
                metodo.getName(), LatidoPulso.TIPO, anotacion.kind(),
                prioridadBus, fase, anotacion.tps());

        return bus.suscribir(LatidoPulso.TIPO, prioridadBus, fase, ejecutor, oyente);
    }

    @SuppressWarnings("unchecked")
    private static <E> Latido<E> resolverLatido(Method metodo,
                                                 Class<E> tipoEvento,
                                                 Class<?> claseOwner) {
        List<Latido<E>> candidatos = new ArrayList<>();

        buscarLatidoEnClase(tipoEvento, tipoEvento, candidatos);

        if (candidatos.isEmpty()) {
            buscarLatidoEnClase(claseOwner, tipoEvento, candidatos);
        }

        if (candidatos.isEmpty()) {
            throw new LatidoRegistradorError.LatidoNoEncontrado(metodo, tipoEvento);
        }
        if (candidatos.size() > 1) {
            throw new LatidoRegistradorError.LatidoAmbiguo(metodo, tipoEvento, candidatos.size());
        }
        return candidatos.getFirst();
    }

    @SuppressWarnings("unchecked")
    private static <E> void buscarLatidoEnClase(Class<?> donde,
                                                 Class<E> tipoEvento,
                                                 List<Latido<E>> acumulador) {
        for (Field f : donde.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) continue;
            if (!Modifier.isFinal(f.getModifiers())) continue;
            if (!Latido.class.isAssignableFrom(f.getType())) continue;

            try {
                f.setAccessible(true);
                Object valor = f.get(null);
                if (valor instanceof Latido<?> lat
                        && lat.claseEvento().isAssignableFrom(tipoEvento)) {
                    acumulador.add((Latido<E>) lat);
                }
            } catch (IllegalAccessException ignore) {
                // skip inaccessible fields
            }
        }
    }

    private static Ejecutor crearEjecutor(Method metodo,
                                          EjecutorLatido anotacion,
                                          Susurro susurro,
                                          HiloPrincipal hp) {
        return crearEjecutor(
                metodo,
                anotacion.kind(),
                anotacion.prioridad(),
                anotacion.etiqueta(),
                susurro,
                hp,
                "@EjecutorLatido");
    }

    private static Ejecutor crearEjecutor(Method metodo,
                                          EjecutorLatido.Kind kind,
                                          EjecutorLatido.Prioridad prioridad,
                                          String etiqueta,
                                          Susurro susurro,
                                          HiloPrincipal hp,
                                          String anotacion) {
        return switch (kind) {
            case SINCRONO -> Ejecutor.SINCRONO;

            case SUSURRO -> {
                if (susurro == null) {
                    throw new LatidoRegistradorError.EjecutorFaltante(
                            metodo, anotacion, EjecutorLatido.Kind.SUSURRO);
                }
                yield Ejecutor.susurro(susurro,
                        prioridad.aSusurro(),
                        Etiqueta.de(etiqueta));
            }

            case HILO_PRINCIPAL -> {
                if (hp == null) {
                    throw new LatidoRegistradorError.EjecutorFaltante(
                            metodo, anotacion, EjecutorLatido.Kind.HILO_PRINCIPAL);
                }
                yield Ejecutor.hiloPrincipal(hp);
            }
        };
    }

    private static boolean debeEjecutarTick(long tickActual, int tps) {
        if (tickActual == 0L) {
            return true;
        }
        long ahora = (tickActual * tps) / 20L;
        long antes = ((tickActual - 1L) * tps) / 20L;
        return ahora != antes;
    }

    /**
     * Пытается получить привязанный {@link MethodHandle} для вызова
     * {@code instance.metodo(evento)} без {@code Method.invoke}.
     */
    private static MethodHandle manijaInstancia(Method metodo, Object instance) {
        try {
            return MethodHandles.lookup().unreflect(metodo).bindTo(instance);
        } catch (IllegalAccessException | SecurityException e) {
            return null;
        }
    }

    private static void invocarManija(MethodHandle manija, Object evento) {
        try {
            manija.invoke(evento);
        } catch (Throwable t) {
            if (t instanceof RuntimeException re) {
                throw re;
            }
            if (t instanceof Error err) {
                throw err;
            }
            throw new RuntimeException(t);
        }
    }

    private static void invocarMetodo(Object instance, Method metodo, Object evento) {
        try {
            metodo.invoke(instance, evento);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error err) throw err;
            throw new RuntimeException(cause);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
