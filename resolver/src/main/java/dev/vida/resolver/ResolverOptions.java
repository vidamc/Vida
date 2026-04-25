/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import dev.vida.core.ApiStatus;
import dev.vida.core.Version;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Настройки резолвера: стратегия выбора, pin’ы, exclude’ы,
 * {@linkplain #accessDeniedIds() политика доступа}, лимиты.
 *
 * <p>Экземпляры неизменяемы и дёшевы в создании — используйте
 * семейство {@code with*} методов для точечных правок.
 */
@ApiStatus.Stable
public final class ResolverOptions {

    /**
     * Дефолты: NEWEST, без пинов/excludes/deny, 50000 решений, 2 секунды таймаут.
     */
    public static final ResolverOptions DEFAULTS = new ResolverOptions(
            ResolverStrategy.NEWEST,
            Map.of(),
            Set.of(),
            Set.of(),
            /* skipOptional = */ false,
            /* maxDecisions = */ 50_000,
            /* timeoutMillis = */ 2_000L);

    private final ResolverStrategy strategy;
    private final Map<String, Version> pins;
    private final Set<String> excludes;
    /** Провайдеры с таким {@link Provider#id()} никогда не попадают в {@link Resolution}. */
    private final Set<String> accessDeniedIds;
    private final boolean skipOptional;
    private final int maxDecisions;
    private final long timeoutMillis;

    private ResolverOptions(ResolverStrategy strategy,
                            Map<String, Version> pins,
                            Set<String> excludes,
                            Set<String> accessDeniedIds,
                            boolean skipOptional,
                            int maxDecisions,
                            long timeoutMillis) {
        if (maxDecisions <= 0) {
            throw new IllegalArgumentException("maxDecisions must be > 0");
        }
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis must be > 0");
        }
        this.strategy = Objects.requireNonNull(strategy, "strategy");
        this.pins = Map.copyOf(Objects.requireNonNull(pins, "pins"));
        this.excludes = Set.copyOf(Objects.requireNonNull(excludes, "excludes"));
        this.accessDeniedIds = copyDeniedIds(accessDeniedIds);
        this.skipOptional = skipOptional;
        this.maxDecisions = maxDecisions;
        this.timeoutMillis = timeoutMillis;
    }

    public ResolverStrategy strategy()           { return strategy; }
    public Map<String, Version> pins()           { return pins; }
    public Set<String> excludes()                { return excludes; }

    /**
     * Идентификаторы модов, которые резолвер не имеет права выбирать (политика лаунчера /
     * администратора). Отличается от {@link #excludes()}: exclude — мягкий пропуск при
     * отсутствии жёстких требований; deny — кандидат с этим {@code id} отфильтровывается
     * всегда. Если жёсткая зависимость требует запрещённый мод — {@link ResolverError.AccessPolicyDenied}.
     */
    public Set<String> accessDeniedIds()        { return accessDeniedIds; }

    public boolean skipOptional()                { return skipOptional; }
    public int maxDecisions()                    { return maxDecisions; }
    public long timeoutMillis()                  { return timeoutMillis; }

    // ------------------------------------------------------------ mutators

    public ResolverOptions withStrategy(ResolverStrategy s) {
        return new ResolverOptions(s, pins, excludes, accessDeniedIds, skipOptional, maxDecisions, timeoutMillis);
    }

    public ResolverOptions withPin(String id, Version v) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(v, "version");
        Map<String, Version> next = new LinkedHashMap<>(pins);
        next.put(id, v);
        return new ResolverOptions(strategy, next, excludes, accessDeniedIds, skipOptional, maxDecisions, timeoutMillis);
    }

    public ResolverOptions withPins(Map<String, Version> v) {
        return new ResolverOptions(strategy, v, excludes, accessDeniedIds, skipOptional, maxDecisions, timeoutMillis);
    }

    public ResolverOptions withExclude(String id) {
        Objects.requireNonNull(id, "id");
        Set<String> next = new LinkedHashSet<>(excludes);
        next.add(id);
        return new ResolverOptions(strategy, pins, next, accessDeniedIds, skipOptional, maxDecisions, timeoutMillis);
    }

    public ResolverOptions withExcludes(Set<String> v) {
        return new ResolverOptions(strategy, pins, v, accessDeniedIds, skipOptional, maxDecisions, timeoutMillis);
    }

    /**
     * Запрещает выбор любого провайдера с данным {@link Provider#id()} (политика доступа).
     */
    public ResolverOptions withAccessDenied(String id) {
        Objects.requireNonNull(id, "id");
        Set<String> next = new LinkedHashSet<>(accessDeniedIds);
        next.add(id);
        return new ResolverOptions(strategy, pins, excludes, next, skipOptional, maxDecisions, timeoutMillis);
    }

    public ResolverOptions withAccessDenied(Set<String> ids) {
        Set<String> next = new LinkedHashSet<>(accessDeniedIds);
        for (String id : Objects.requireNonNull(ids, "ids")) {
            if (id != null && !id.isBlank()) {
                next.add(id);
            }
        }
        return new ResolverOptions(strategy, pins, excludes, Set.copyOf(next), skipOptional, maxDecisions, timeoutMillis);
    }

    public ResolverOptions withSkipOptional(boolean v) {
        return new ResolverOptions(strategy, pins, excludes, accessDeniedIds, v, maxDecisions, timeoutMillis);
    }

    public ResolverOptions withMaxDecisions(int v) {
        return new ResolverOptions(strategy, pins, excludes, accessDeniedIds, skipOptional, v, timeoutMillis);
    }

    public ResolverOptions withTimeoutMillis(long v) {
        return new ResolverOptions(strategy, pins, excludes, accessDeniedIds, skipOptional, maxDecisions, v);
    }

    private static Set<String> copyDeniedIds(Set<String> raw) {
        Set<String> out = new LinkedHashSet<>();
        for (String id : Objects.requireNonNull(raw, "accessDeniedIds")) {
            if (id != null && !id.isBlank()) {
                out.add(id);
            }
        }
        return Set.copyOf(out);
    }

    @Override
    public String toString() {
        return "ResolverOptions[" + strategy
                + ", pins=" + pins.size()
                + ", excludes=" + excludes.size()
                + ", accessDenied=" + accessDeniedIds.size()
                + ", skipOptional=" + skipOptional
                + ", maxDecisions=" + maxDecisions
                + ", timeoutMillis=" + timeoutMillis + "]";
    }

    /** Служебно: read-only view для внутренних тестов. */
    Set<String> excludesView() { return Collections.unmodifiableSet(excludes); }
}
