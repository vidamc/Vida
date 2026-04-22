/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import dev.vida.core.ApiStatus;
import dev.vida.core.Version;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Иммутабельный каталог {@link Provider}'ов, проиндексированный по id и
 * по aliases (поле {@code provides}).
 *
 * <p>Универс гарантирует, что для каждой пары {@code (id, version)} будет
 * храниться не более одного провайдера. Попытка добавить второго
 * сразу даёт {@link ResolverError.DuplicateProvider} на этапе постройки.
 */
@ApiStatus.Stable
public final class Universe {

    /** key → список провайдеров, сортированный по убыванию версии. */
    private final Map<String, List<Provider>> byKey;
    private final List<Provider> all;

    private Universe(Map<String, List<Provider>> byKey, List<Provider> all) {
        this.byKey = byKey;
        this.all = all;
    }

    public List<Provider> all() { return all; }

    /**
     * Возвращает все провайдеры, отвечающие на заданный id (собственный или
     * алиас). Список упорядочен по убыванию версии; безопасно к чтению.
     */
    public List<Provider> candidates(String queryId) {
        Objects.requireNonNull(queryId, "queryId");
        List<Provider> xs = byKey.get(queryId);
        return xs == null ? List.of() : xs;
    }

    /** Множество всех известных id (включая алиасы). */
    public Set<String> knownIds() {
        return Collections.unmodifiableSet(byKey.keySet());
    }

    public int size() { return all.size(); }

    public boolean isEmpty() { return all.isEmpty(); }

    // ------------------------------------------------------------ builders

    public static Builder builder() { return new Builder(); }

    public static Universe of(Collection<Provider> providers) {
        Objects.requireNonNull(providers, "providers");
        Builder b = builder();
        for (Provider p : providers) b.add(p);
        return b.build();
    }

    public static final class Builder {

        private final Map<String, Map<Version, Provider>> slots = new LinkedHashMap<>();
        private final List<Provider> insertion = new ArrayList<>();

        private Builder() {}

        /**
         * Добавляет провайдер. Если в универсе уже есть провайдер с тем же
         * собственным id и той же версией, бросает {@link IllegalStateException} —
         * такой конфликт входных данных должен быть обработан выше по стеку
         * (например, дискавери-слоем, который сам решает, что делать с
         * дублями на диске).
         */
        public Builder add(Provider p) {
            Objects.requireNonNull(p, "provider");
            Map<Version, Provider> bucket = slots.computeIfAbsent(
                    p.id(), k -> new HashMap<>());
            Provider prev = bucket.putIfAbsent(p.version(), p);
            if (prev != null && prev != p) {
                throw new IllegalStateException(
                        "duplicate provider for " + p.id() + "@" + p.version());
            }
            if (prev == null) insertion.add(p);
            return this;
        }

        public Universe build() {
            // Построим индекс id|alias → отсортированный список провайдеров.
            Map<String, List<Provider>> byKey = new LinkedHashMap<>();
            List<Provider> allList = List.copyOf(insertion);

            Comparator<Provider> byVerDesc = Comparator
                    .<Provider, Version>comparing(Provider::version)
                    .reversed();

            for (Provider p : allList) {
                addTo(byKey, p.id(), p);
                for (String alias : p.provides()) {
                    addTo(byKey, alias, p);
                }
            }
            // Стабилизируем порядок и делаем read-only.
            Map<String, List<Provider>> frozen = new LinkedHashMap<>(byKey.size());
            for (Map.Entry<String, List<Provider>> e : byKey.entrySet()) {
                List<Provider> xs = dedup(e.getValue());
                xs.sort(byVerDesc);
                frozen.put(e.getKey(), List.copyOf(xs));
            }
            return new Universe(Collections.unmodifiableMap(frozen), allList);
        }

        private static void addTo(Map<String, List<Provider>> map, String k, Provider p) {
            map.computeIfAbsent(k, ignored -> new ArrayList<>()).add(p);
        }

        /**
         * Убирает дубли provider’а (может появиться, если его id встретился и как
         * id, и в {@code provides} у кого-то другого с таким же именем).
         * Сохраняем первый экземпляр в порядке добавления.
         */
        private static List<Provider> dedup(List<Provider> xs) {
            if (xs.size() < 2) return xs;
            Set<Provider> seen = new LinkedHashSet<>(xs.size() * 2);
            seen.addAll(xs);
            return new ArrayList<>(seen);
        }
    }
}
