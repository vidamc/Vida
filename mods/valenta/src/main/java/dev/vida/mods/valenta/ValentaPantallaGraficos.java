/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta;

import dev.vida.core.ApiStatus;
import dev.vida.mods.valenta.quality.CloudRenderer;
import dev.vida.mods.valenta.quality.ParticleFilter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Construye la lista de opciones de vídeo: bloque Valenta y luego vanilla,
 * usando reflexión para no enlazar el JAR del cliente en tiempo de compilación.
 */
@ApiStatus.Internal
public final class ValentaPantallaGraficos {

    private static final Logger LOG = LoggerFactory.getLogger(ValentaPantallaGraficos.class);

    private static final String CLS_VIDEO = "net.minecraft.client.gui.screens.options.VideoSettingsScreen";
    private static final String CLS_OPTION = "net.minecraft.client.OptionInstance";
    private static final String CLS_OPTIONS = "net.minecraft.client.Options";
    private static final String CLS_COMPONENT = "net.minecraft.network.chat.Component";
    private static final String CLS_STRING_WIDGET = "net.minecraft.client.gui.components.StringWidget";
    private static final String CLS_ABSTRACT_WIDGET = "net.minecraft.client.gui.components.AbstractWidget";

    private ValentaPantallaGraficos() {}

    /**
     * Equivalente a {@code VideoSettingsScreen#addOptions} con prefijo Valenta.
     */
    public static void reemplazarOpcionesVideo(Object pantalla) {
        try {
            Object lista = buscarCampo(pantalla, "list");
            Object opciones = buscarCampo(pantalla, "options");
            if (lista == null || opciones == null) {
                LOG.warn("Valenta: no se pudo resolver list/options en pantalla de vídeo");
                return;
            }
            Method clear = buscarMetodo(lista.getClass(), "clearEntries");
            if (clear != null) {
                clear.invoke(lista);
            }

            Object fuente = buscarCampo(pantalla, "font");
            if (fuente == null) {
                Object mc = buscarCampo(pantalla, "minecraft");
                if (mc != null) {
                    fuente = buscarCampo(mc, "font");
                }
            }

            int ancho = 400;
            Object w = buscarCampo(pantalla, "width");
            if (w instanceof Integer iw && iw > 0) {
                ancho = Math.min(iw - 40, 420);
            }

            if (fuente != null) {
                addSeparadorTexto(lista, pantalla, fuente, ancho, "— Valenta —");
            }

            ValentaMod mod = ValentaRuntime.instancia();
            Class<?> optCls = Class.forName(CLS_OPTION);
            Method addBig = lista.getClass().getMethod("addBig", optCls);
            Method createBoolean = optCls.getMethod(
                    "createBoolean", String.class, boolean.class, Consumer.class);

            if (mod == null) {
                if (fuente != null) {
                    addSeparadorTexto(lista, pantalla, fuente, ancho, "Valenta: mod no inicializado");
                }
            } else {
                List<Object> filasValenta = new ArrayList<>(6);

                filasValenta.add(createBoolean.invoke(null,
                        "valenta.gpu_timings",
                        mod.timingPane().isVisible(),
                        (Consumer<Boolean>) v -> aplicarDesdeFilas(filasValenta, mod)));
                addBig.invoke(lista, filasValenta.get(filasValenta.size() - 1));

                filasValenta.add(createBoolean.invoke(null,
                        "valenta.occlusion_overlay",
                        mod.occlusionOverlay().isVisible(),
                        (Consumer<Boolean>) v -> aplicarDesdeFilas(filasValenta, mod)));
                addBig.invoke(lista, filasValenta.get(filasValenta.size() - 1));

                boolean ocultarPart = mod.particleFilter().mode() == ParticleFilter.Mode.OCULTAR;
                filasValenta.add(createBoolean.invoke(null,
                        "valenta.particles_hide",
                        ocultarPart,
                        (Consumer<Boolean>) v -> aplicarDesdeFilas(filasValenta, mod)));
                addBig.invoke(lista, filasValenta.get(filasValenta.size() - 1));

                boolean reducir = mod.particleFilter().mode() == ParticleFilter.Mode.REDUCIR;
                filasValenta.add(createBoolean.invoke(null,
                        "valenta.particles_reduce",
                        reducir,
                        (Consumer<Boolean>) v -> aplicarDesdeFilas(filasValenta, mod)));
                addBig.invoke(lista, filasValenta.get(filasValenta.size() - 1));

                boolean sinNubes = mod.cloudRenderer().mode() == CloudRenderer.Mode.DESACTIVAR;
                filasValenta.add(createBoolean.invoke(null,
                        "valenta.clouds_off",
                        sinNubes,
                        (Consumer<Boolean>) v -> aplicarDesdeFilas(filasValenta, mod)));
                addBig.invoke(lista, filasValenta.get(filasValenta.size() - 1));

                boolean nubesRapidas = mod.cloudRenderer().mode() == CloudRenderer.Mode.RAPIDAS;
                filasValenta.add(createBoolean.invoke(null,
                        "valenta.clouds_fast",
                        nubesRapidas,
                        (Consumer<Boolean>) v -> aplicarDesdeFilas(filasValenta, mod)));
                addBig.invoke(lista, filasValenta.get(filasValenta.size() - 1));
            }

            if (fuente != null) {
                addSeparadorTexto(lista, pantalla, fuente, ancho, "— Minecraft —");
            }

            Object[] vanilla = opcionesVanillaVideo(pantalla, opciones);
            if (vanilla != null) {
                for (Object oi : vanilla) {
                    if (oi != null) {
                        addBig.invoke(lista, oi);
                    }
                }
            }
        } catch (ReflectiveOperationException | ClassCastException e) {
            LOG.warn("Valenta: error al montar opciones de vídeo: {}", e.toString());
        }
    }

    private static void aplicarDesdeFilas(List<Object> filas, ValentaMod mod) {
        if (filas.size() < 6) {
            return;
        }
        try {
            Method get = Class.forName(CLS_OPTION).getMethod("get");
            boolean gpu = (Boolean) get.invoke(filas.get(0));
            boolean occ = (Boolean) get.invoke(filas.get(1));
            boolean partOcultar = (Boolean) get.invoke(filas.get(2));
            boolean partReducir = (Boolean) get.invoke(filas.get(3));
            boolean sinNubes = (Boolean) get.invoke(filas.get(4));
            boolean nubesRapidas = (Boolean) get.invoke(filas.get(5));
            mod.aplicarAjustesDesdePantallaVideo(gpu, occ, partOcultar, partReducir, sinNubes, nubesRapidas);
        } catch (ReflectiveOperationException e) {
            LOG.debug("Valenta: sync pantalla", e);
        }
    }

    private static Object[] opcionesVanillaVideo(Object pantalla, Object opcionesMc)
            throws ReflectiveOperationException {
        Class<?> vss = Class.forName(CLS_VIDEO);
        Class<?> optsCls = Class.forName(CLS_OPTIONS);
        for (Method m : vss.getDeclaredMethods()) {
            if (!"options".equals(m.getName())) {
                continue;
            }
            Class<?>[] pt = m.getParameterTypes();
            if (pt.length != 1 || !pt[0].equals(optsCls)) {
                continue;
            }
            m.setAccessible(true);
            Object r = Modifier.isStatic(m.getModifiers())
                    ? m.invoke(null, opcionesMc)
                    : m.invoke(pantalla, opcionesMc);
            if (r == null) {
                return null;
            }
            if (r.getClass().isArray()) {
                int n = Array.getLength(r);
                Object[] out = new Object[n];
                for (int i = 0; i < n; i++) {
                    out[i] = Array.get(r, i);
                }
                return out;
            }
            return null;
        }
        LOG.warn("Valenta: VideoSettingsScreen.options(Options) no encontrado");
        return null;
    }

    private static void addSeparadorTexto(
            Object lista, Object pantalla, Object fuente, int ancho, String texto)
            throws ReflectiveOperationException {
        Method literal = Class.forName(CLS_COMPONENT).getMethod("literal", String.class);
        Object componente = literal.invoke(null, texto);
        Class<?> swCls = Class.forName(CLS_STRING_WIDGET);
        Object etiqueta = swCls
                .getConstructor(int.class, int.class, int.class, int.class,
                        Class.forName(CLS_COMPONENT),
                        fuente.getClass())
                .newInstance(0, 0, ancho, 9, componente, fuente);
        Class<?> awCls = Class.forName(CLS_ABSTRACT_WIDGET);
        Method addSmall = lista.getClass().getMethod("addSmall", awCls, awCls);
        addSmall.invoke(lista, etiqueta, etiqueta);
    }

    private static Object buscarCampo(Object o, String nombre) {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(nombre);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException ignored) {
                // siguiente superclase
            } catch (ReflectiveOperationException e) {
                LOG.debug("Valenta: campo {}: {}", nombre, e.toString());
                return null;
            }
        }
        return null;
    }

    private static Method buscarMetodo(Class<?> c, String nombre) {
        for (Class<?> x = c; x != null; x = x.getSuperclass()) {
            for (Method m : x.getDeclaredMethods()) {
                if (m.getName().equals(nombre) && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }
}
