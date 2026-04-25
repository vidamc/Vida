/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.platform;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * Кэш {@code GuiGraphics.getFont} + {@code drawString(Font, String, int, int, int, boolean)} —
 * без {@code Method.invoke} на кадр (см. ТЗ Vida).
 */
final class GuiGraphicsDrawStringReflect {

    private static volatile MethodHandle obtenerFuente;
    private static volatile MethodHandle dibujarCadena;

    private GuiGraphicsDrawStringReflect() {}

    static void dibujar(Object guiGraphics, String texto, int x, int y, int colorArgb) {
        if (guiGraphics == null || texto == null || texto.isEmpty()) {
            return;
        }
        try {
            MethodHandle fuenteMh = fuenteMh(guiGraphics);
            MethodHandle cadenaMh = cadenaMh(guiGraphics);
            if (fuenteMh == null || cadenaMh == null) {
                return;
            }
            Object font = fuenteMh.invoke(guiGraphics);
            if (font == null) {
                return;
            }
            cadenaMh.invoke(guiGraphics, font, texto, x, y, colorArgb, false);
        } catch (Throwable ignored) {
            // несовместимая версия MC — один кадр без текста
        }
    }

    private static MethodHandle fuenteMh(Object muestra) throws ReflectiveOperationException {
        MethodHandle mh = obtenerFuente;
        if (mh != null) {
            return mh;
        }
        synchronized (GuiGraphicsDrawStringReflect.class) {
            if (obtenerFuente != null) {
                return obtenerFuente;
            }
            Method m = muestra.getClass().getMethod("getFont");
            obtenerFuente = MethodHandles.publicLookup().unreflect(m);
            return obtenerFuente;
        }
    }

    private static MethodHandle cadenaMh(Object muestra) throws ReflectiveOperationException {
        MethodHandle mh = dibujarCadena;
        if (mh != null) {
            return mh;
        }
        synchronized (GuiGraphicsDrawStringReflect.class) {
            if (dibujarCadena != null) {
                return dibujarCadena;
            }
            Class<?> fontCl = Class.forName("net.minecraft.client.gui.Font");
            Method m = muestra.getClass().getMethod(
                    "drawString",
                    fontCl,
                    String.class,
                    int.class,
                    int.class,
                    int.class,
                    boolean.class);
            dibujarCadena = MethodHandles.publicLookup().unreflect(m);
            return dibujarCadena;
        }
    }

    static void resetForTests() {
        synchronized (GuiGraphicsDrawStringReflect.class) {
            obtenerFuente = null;
            dibujarCadena = null;
        }
    }
}
