/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver.internal;

import dev.vida.core.ApiStatus;
import dev.vida.resolver.Provider;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Мутабельное состояние поиска — передаётся по ссылке между шагами
 * бэктрекинга и снапшотится на каждой развилке.
 *
 * <p>Снапшоты выполнены через неглубокое клонирование коллекций: память
 * ~O(N) на уровень рекурсии, что приемлемо для типичного размера модпаков
 * (сотни-тысячи модов).
 */
@ApiStatus.Internal
public final class SearchState {

    /** id → выбранный провайдер. Ключ — собственный id провайдера. */
    public Map<String, Provider> selected = new LinkedHashMap<>();

    /**
     * queryId → провайдер, отвечающий за этот id. queryId может быть
     * собственным id провайдера или одним из его {@code provides}.
     * Нужен, чтобы не выбирать разные провайдеры на одну и ту же роль.
     */
    public Map<String, Provider> byQuery = new LinkedHashMap<>();

    /** queryId → список накопленных REQUIRED/OPTIONAL ограничений. */
    public Map<String, List<Constraint>> rangeConstraints = new LinkedHashMap<>();

    /** queryId → список накопленных INCOMPATIBLE ограничений. */
    public Map<String, List<Constraint>> incompatConstraints = new LinkedHashMap<>();

    /** Id, для которых ещё нужно выбрать провайдер. Порядок — FIFO. */
    public LinkedHashSet<String> pending = new LinkedHashSet<>();

    public LinkedHashSet<String> optionalIncluded = new LinkedHashSet<>();
    public LinkedHashSet<String> optionalMissing  = new LinkedHashSet<>();

    public SearchState snapshot() {
        SearchState s = new SearchState();
        s.selected            = new LinkedHashMap<>(this.selected);
        s.byQuery             = new LinkedHashMap<>(this.byQuery);
        s.rangeConstraints    = copyListMap(this.rangeConstraints);
        s.incompatConstraints = copyListMap(this.incompatConstraints);
        s.pending             = new LinkedHashSet<>(this.pending);
        s.optionalIncluded    = new LinkedHashSet<>(this.optionalIncluded);
        s.optionalMissing     = new LinkedHashSet<>(this.optionalMissing);
        return s;
    }

    public void restoreFrom(SearchState s) {
        this.selected            = s.selected;
        this.byQuery             = s.byQuery;
        this.rangeConstraints    = s.rangeConstraints;
        this.incompatConstraints = s.incompatConstraints;
        this.pending             = s.pending;
        this.optionalIncluded    = s.optionalIncluded;
        this.optionalMissing     = s.optionalMissing;
    }

    public void addRangeConstraint(String id, Constraint c) {
        rangeConstraints.computeIfAbsent(id, k -> new ArrayList<>()).add(c);
    }

    public void addIncompatConstraint(String id, Constraint c) {
        incompatConstraints.computeIfAbsent(id, k -> new ArrayList<>()).add(c);
    }

    public List<Constraint> ranges(String id) {
        List<Constraint> xs = rangeConstraints.get(id);
        return xs == null ? List.of() : xs;
    }

    public List<Constraint> incompats(String id) {
        List<Constraint> xs = incompatConstraints.get(id);
        return xs == null ? List.of() : xs;
    }

    private static Map<String, List<Constraint>> copyListMap(Map<String, List<Constraint>> src) {
        Map<String, List<Constraint>> out = new LinkedHashMap<>(src.size());
        for (Map.Entry<String, List<Constraint>> e : src.entrySet()) {
            out.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return out;
    }
}
