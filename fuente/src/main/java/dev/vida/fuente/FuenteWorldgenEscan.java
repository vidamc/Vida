/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.fuente;

import dev.vida.core.Identifier;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Эвристический обход JSON worldgen/datapack: ключи {@code loot_table}, {@code block},
 * {@code blocks}, {@code Name}+{@code Properties} (block state).
 */
final class FuenteWorldgenEscan {

    private FuenteWorldgenEscan() {}

    static void escanear(Object nodo, String defaultNs, Set<Identifier> loot, Set<Identifier> bloques) {
        if (nodo instanceof Map<?, ?> m) {
            escanearMapa(m, defaultNs, loot, bloques);
        } else if (nodo instanceof List<?> lista) {
            for (Object x : lista) {
                escanear(x, defaultNs, loot, bloques);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void escanearMapa(Map<?, ?> m, String defaultNs, Set<Identifier> loot, Set<Identifier> bloques) {
        for (Map.Entry<?, ?> e : m.entrySet()) {
            String clave = String.valueOf(e.getKey());
            Object valor = e.getValue();

            if ("loot_table".equals(clave) && valor instanceof String s) {
                loot.add(Identifier.parseWithDefault(s, defaultNs));
            }
            if ("block".equals(clave) && valor instanceof String s) {
                bloques.add(Identifier.parseWithDefault(s, defaultNs));
            }
            if ("blocks".equals(clave) && valor instanceof List<?> bl) {
                for (Object x : bl) {
                    if (x instanceof String s) {
                        bloques.add(Identifier.parseWithDefault(s, defaultNs));
                    }
                }
            }
            if ("Name".equals(clave) && valor instanceof String s && m.containsKey("Properties")) {
                bloques.add(Identifier.parseWithDefault(s, defaultNs));
            }

            escanear(valor, defaultNs, loot, bloques);
        }
    }
}
