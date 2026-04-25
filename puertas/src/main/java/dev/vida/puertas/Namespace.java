/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.puertas;

import dev.vida.core.ApiStatus;

/**
 * Namespace маппинга, в котором записаны имена в .ptr-файле.
 *
 * <ul>
 *   <li>{@link #CRUDO} — obfuscated имена (как в релиз-jar без маппингов);</li>
 *   <li>{@link #INTERMEDIO} — стабильные промежуточные имена Vida
 *       (Cartografía .ctg), рекомендуемый формат для .ptr;</li>
 *   <li>{@link #EXTERIOR} — Mojang-mapped (для dev-режима и для модов,
 *       собранных напрямую против Mojang mappings).</li>
 * </ul>
 */
@ApiStatus.Stable
public enum Namespace {
    CRUDO("crudo"),
    INTERMEDIO("intermedio"),
    EXTERIOR("exterior");

    private final String clave;

    Namespace(String clave) {
        this.clave = clave;
    }

    /** Строковый ключ, как пишется в заголовке .ptr. */
    public String clave() { return clave; }

    /** Парсинг по ключу; возвращает {@code null} если неизвестен. */
    public static Namespace deClave(String s) {
        if (s == null) return null;
        for (Namespace n : values()) {
            if (n.clave.equals(s)) return n;
        }
        return null;
    }
}
