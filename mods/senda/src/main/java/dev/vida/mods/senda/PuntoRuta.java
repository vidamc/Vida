/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.senda;

import dev.vida.core.ApiStatus;
import java.util.Objects;

/**
 * Неизменяемая запись путевой точки (waypoint).
 *
 * <p>Координаты в системе отсчёта Minecraft: {@code x}/{@code z} —
 * горизонтальные, {@code y} — вертикальная (высота).
 *
 * <h2>Идентификация</h2>
 * {@link #nombre()} — уникальный ключ внутри одного измерения.
 * Разные размерности могут иметь точки с одним именем.
 *
 * @param nombre    уникальное название точки (не пустое)
 * @param dimension идентификатор измерения ({@code "overworld"}, {@code "nether"}, {@code "end"})
 * @param x         координата X
 * @param y         координата Y (высота)
 * @param z         координата Z
 */
@ApiStatus.Preview("senda")
public record PuntoRuta(String nombre, String dimension, double x, double y, double z) {

    public PuntoRuta {
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(dimension, "dimension");
        if (nombre.isBlank()) throw new IllegalArgumentException("nombre no puede estar vacío");
        if (dimension.isBlank()) throw new IllegalArgumentException("dimension no puede estar vacía");
    }

    /**
     * Calcula la distancia horizontal (XZ) a otro punto de la misma dimensión.
     *
     * @param otro otro punto de la misma dimensión
     * @return distancia en bloques; {@link Double#NaN} si las dimensiones difieren
     */
    public double distanciaHorizontal(PuntoRuta otro) {
        Objects.requireNonNull(otro, "otro");
        if (!dimension.equals(otro.dimension)) return Double.NaN;
        double dx = x - otro.x;
        double dz = z - otro.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Calcula la distancia 3D a otro punto de la misma dimensión.
     *
     * @param otro otro punto de la misma dimensión
     * @return distancia en bloques; {@link Double#NaN} si las dimensiones difieren
     */
    public double distancia3d(PuntoRuta otro) {
        Objects.requireNonNull(otro, "otro");
        if (!dimension.equals(otro.dimension)) return Double.NaN;
        double dx = x - otro.x;
        double dy = y - otro.y;
        double dz = z - otro.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public String toString() {
        return "PuntoRuta[" + nombre + " @ " + dimension
                + " (" + x + ", " + y + ", " + z + ")]";
    }
}
