/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.bloque;

import dev.vida.core.ApiStatus;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Форма коллизии / outline блока — объединение AABB.
 *
 * <p>Координаты — в «блочных» единицах (0..1 по каждой оси). Несколько
 * прямоугольных параллелепипедов объединяются через {@link #union(FormaColision...)};
 * это позволяет описать любой сложный блок (забор, стена, лестница) без
 * отдельного DSL.
 *
 * <p>Класс иммутабельный и thread-safe. Для VoxelShape-семантики (бит-маска
 * 16×16×16) существуют адаптеры в будущем {@code vida-mundo}, но сама модель
 * проще и годится для 99% пользовательских блоков.
 *
 * <h2>Примеры</h2>
 * <pre>{@code
 *   // Полный блок
 *   FormaColision lleno = FormaColision.completo();
 *
 *   // Ступенька (половина высоты)
 *   FormaColision paso = FormaColision.aabb(0, 0, 0,  1, 0.5, 1);
 *
 *   // Лестница = нижняя половина + задняя четверть
 *   FormaColision escalera = FormaColision.union(
 *           FormaColision.aabb(0, 0, 0, 1, 0.5, 1),
 *           FormaColision.aabb(0, 0.5, 0, 1, 1.0, 0.5));
 * }</pre>
 */
@ApiStatus.Preview("bloque")
public final class FormaColision {

    /** Ограничивающий ящик. Координаты в диапазоне [0..1]. */
    public record Caja(double minX, double minY, double minZ,
                       double maxX, double maxY, double maxZ) {
        public Caja {
            if (minX > maxX || minY > maxY || minZ > maxZ) {
                throw new IllegalArgumentException(
                        "caja: min > max (" + minX + "," + minY + "," + minZ + " ↔ "
                                + maxX + "," + maxY + "," + maxZ + ")");
            }
            if (minX < -1.0 || maxX > 2.0
                    || minY < -1.0 || maxY > 2.0
                    || minZ < -1.0 || maxZ > 2.0) {
                // vanilla допускает выход за пределы блока, но не более одного блока
                // в каждую сторону (заборы, ступени до соседа). Ограничение защищает
                // от случайных ошибок единиц измерения.
                throw new IllegalArgumentException(
                        "caja выходит за допустимый диапазон [-1..2]: "
                                + minX + "," + minY + "," + minZ + " ↔ "
                                + maxX + "," + maxY + "," + maxZ);
            }
        }

        /** Объём AABB (для тестов и сортировки). */
        public double volumen() {
            return (maxX - minX) * (maxY - minY) * (maxZ - minZ);
        }
    }

    /** Пустая форма — блок не коллидирует. */
    public static final FormaColision VACIO = new FormaColision(List.of());

    /** Полный блок 1×1×1. */
    public static final FormaColision COMPLETO =
            new FormaColision(List.of(new Caja(0, 0, 0, 1, 1, 1)));

    private final List<Caja> cajas;

    private FormaColision(List<Caja> cajas) {
        this.cajas = cajas;
    }

    // ------------------------------------------------------------------ factory

    /** Форма одного AABB. */
    public static FormaColision aabb(double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ) {
        return new FormaColision(List.of(
                new Caja(minX, minY, minZ, maxX, maxY, maxZ)));
    }

    /** Полный блок. */
    public static FormaColision completo() { return COMPLETO; }

    /** Пустая форма. */
    public static FormaColision vacio() { return VACIO; }

    /** Объединение нескольких форм. */
    public static FormaColision union(FormaColision... partes) {
        Objects.requireNonNull(partes, "partes");
        return union(Arrays.asList(partes));
    }

    /** Объединение нескольких форм. */
    public static FormaColision union(Collection<FormaColision> partes) {
        Objects.requireNonNull(partes, "partes");
        List<Caja> todas = new ArrayList<>();
        for (FormaColision p : partes) {
            Objects.requireNonNull(p, "parte");
            todas.addAll(p.cajas);
        }
        return new FormaColision(List.copyOf(todas));
    }

    // ------------------------------------------------------------------ query

    /** Список всех ограничивающих ящиков. */
    public List<Caja> cajas() { return cajas; }

    /** Пустая ли форма. */
    public boolean esVacio() { return cajas.isEmpty(); }

    /** Количество ящиков. */
    public int cantidadCajas() { return cajas.size(); }

    /** Суммарный объём (неоптимизированный — с учётом перекрытий). */
    public double volumen() {
        double v = 0;
        for (Caja c : cajas) v += c.volumen();
        return v;
    }

    /**
     * Внешний AABB — наименьший параллелепипед, содержащий все ящики.
     * {@code null}, если форма пуста.
     */
    public Caja exterior() {
        if (cajas.isEmpty()) return null;
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (Caja c : cajas) {
            if (c.minX < minX) minX = c.minX;
            if (c.minY < minY) minY = c.minY;
            if (c.minZ < minZ) minZ = c.minZ;
            if (c.maxX > maxX) maxX = c.maxX;
            if (c.maxY > maxY) maxY = c.maxY;
            if (c.maxZ > maxZ) maxZ = c.maxZ;
        }
        return new Caja(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** Быстрая проверка точки: попадает ли {@code (x,y,z)} в хотя бы один AABB. */
    public boolean contiene(double x, double y, double z) {
        for (Caja c : cajas) {
            if (x >= c.minX && x <= c.maxX
                    && y >= c.minY && y <= c.maxY
                    && z >= c.minZ && z <= c.maxZ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FormaColision other)) return false;
        return cajas.equals(other.cajas);
    }

    @Override
    public int hashCode() {
        return cajas.hashCode();
    }

    @Override
    public String toString() {
        return "FormaColision" + cajas;
    }
}
