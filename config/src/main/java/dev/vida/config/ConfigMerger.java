/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.config;

import dev.vida.core.ApiStatus;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Глубокое слияние {@link ConfigNode}.
 *
 * <h2>Правила</h2>
 * <ul>
 *   <li><b>Table × Table</b> — рекурсивное слияние; ключи {@code base} сохраняются,
 *       ключи {@code overlay} перекрывают их; новые ключи добавляются.</li>
 *   <li><b>Array × Array</b> — массив заменяется целиком (right-wins). Это
 *       сознательный выбор: «сквозное» слияние массивов ведёт к сюрпризам
 *       (например, нельзя удалить элемент, добавленный в base).</li>
 *   <li><b>Несовпадающие типы</b> — overlay выигрывает. Это используется в
 *       профилях для переопределения, например, {@code renderDistance = 32}
 *       на {@code renderDistance = "auto"}.</li>
 *   <li><b>Scalar × Scalar</b> — overlay выигрывает.</li>
 * </ul>
 *
 * <p>Функция чистая: исходные узлы не мутируются.
 */
@ApiStatus.Stable
public final class ConfigMerger {

    private ConfigMerger() {}

    /** Объединяет два узла верхнего уровня. */
    public static ConfigNode merge(ConfigNode base, ConfigNode overlay) {
        if (base == null) return overlay;
        if (overlay == null) return base;
        if (base instanceof ConfigNode.Table b && overlay instanceof ConfigNode.Table o) {
            return mergeTables(b, o);
        }
        return overlay;
    }

    /**
     * Удобный перегруз для таблиц верхнего уровня.
     */
    public static ConfigNode.Table merge(ConfigNode.Table base, ConfigNode.Table overlay) {
        return mergeTables(base, overlay);
    }

    /**
     * Применяет последовательность overlay-слоёв в порядке их передачи.
     * Эквивалентно левой свёртке {@link #merge(ConfigNode, ConfigNode)}.
     */
    public static ConfigNode.Table layer(ConfigNode.Table base, ConfigNode.Table... overlays) {
        ConfigNode.Table acc = base;
        for (ConfigNode.Table o : overlays) {
            if (o == null) continue;
            acc = mergeTables(acc, o);
        }
        return acc;
    }

    // --------------------------------------------------------------- core

    private static ConfigNode.Table mergeTables(ConfigNode.Table base, ConfigNode.Table overlay) {
        if (base.isEmpty()) return overlay;
        if (overlay.isEmpty()) return base;

        LinkedHashMap<String, ConfigNode> out = new LinkedHashMap<>(base.entries());
        for (Map.Entry<String, ConfigNode> e : overlay.entries().entrySet()) {
            String key = e.getKey();
            ConfigNode overlayVal = e.getValue();
            ConfigNode baseVal = out.get(key);

            if (baseVal instanceof ConfigNode.Table bt && overlayVal instanceof ConfigNode.Table ot) {
                out.put(key, mergeTables(bt, ot));
            } else {
                out.put(key, overlayVal);
            }
        }
        return new ConfigNode.Table(out);
    }
}
