/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.core.ApiStatus;
import dev.vida.vifada.MorphSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Потокобезопасная мапа {@code targetInternal → List<MorphSource>}.
 *
 * <p>Собирается один раз во время бутстрапа из всех дискавери-найденных
 * модов и затем доступна только для чтения через {@link #forTarget(String)}.
 * Для единичных целевых классов лукап — O(1).
 */
@ApiStatus.Preview("loader")
public final class MorphIndex {

    private final Map<String, List<MorphSource>> byTarget;
    private final int totalMorphs;

    private MorphIndex(Map<String, List<MorphSource>> byTarget, int total) {
        this.byTarget = Collections.unmodifiableMap(byTarget);
        this.totalMorphs = total;
    }

    /** Список морфов, таргетящих данный internal-name (например, {@code com/mojang/Foo}). */
    public List<MorphSource> forTarget(String targetInternal) {
        List<MorphSource> list = byTarget.get(targetInternal);
        return list == null ? List.of() : list;
    }

    /** Имеются ли какие-либо морфы на указанный класс. */
    public boolean hasMorphs(String targetInternal) {
        List<MorphSource> list = byTarget.get(targetInternal);
        return list != null && !list.isEmpty();
    }

    public int targetCount() { return byTarget.size(); }
    public int totalMorphs() { return totalMorphs; }

    /** Множество internal-имён всех целевых классов. */
    public java.util.Set<String> targets() { return byTarget.keySet(); }

    public static Builder builder() { return new Builder(); }

    /** Пустой индекс. */
    public static MorphIndex empty() {
        return new MorphIndex(new LinkedHashMap<>(), 0);
    }

    // ==================================================================

    public static final class Builder {
        // LinkedHashMap — сохраняем порядок добавления → детерминированный
        // порядок применения морфов у одной цели (дополнительно аппликатор
        // сортирует их по priority).
        private final Map<String, List<MorphSource>> map = new LinkedHashMap<>();
        private int total;

        public Builder add(String targetInternal, MorphSource source) {
            Objects.requireNonNull(targetInternal, "targetInternal");
            Objects.requireNonNull(source, "source");
            map.computeIfAbsent(targetInternal, k -> new ArrayList<>()).add(source);
            total++;
            return this;
        }

        public MorphIndex build() {
            Map<String, List<MorphSource>> frozen = new LinkedHashMap<>(map.size() * 2);
            for (var e : map.entrySet()) frozen.put(e.getKey(), List.copyOf(e.getValue()));
            return new MorphIndex(frozen, total);
        }
    }
}
