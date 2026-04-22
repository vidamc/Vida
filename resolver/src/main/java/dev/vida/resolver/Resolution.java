/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import dev.vida.core.ApiStatus;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Успешный результат резолва.
 *
 * <p>Ключ в {@link #selected()} — это {@linkplain Provider#id() собственный id}
 * провайдера. Если мод заявлен как {@code provides=[alias]}, он всё равно
 * появится под своим родным id (поиск по алиасу смотри в {@link #findByQuery}).
 */
@ApiStatus.Stable
public final class Resolution {

    private final Map<String, Provider> selected;
    private final Set<String> optionalIncluded;
    private final Set<String> optionalMissing;
    private final int decisions;
    private final Duration duration;

    /**
     * Пакетно-внутренний конструктор; пользователи не создают Resolution вручную,
     * её всегда возвращает {@link Resolver}. Публичен, чтобы к нему могла
     * обратиться реализация в подпакете {@code internal}.
     */
    public Resolution(Map<String, Provider> selected,
                      Set<String> optionalIncluded,
                      Set<String> optionalMissing,
                      int decisions,
                      Duration duration) {
        this.selected         = Collections.unmodifiableMap(new LinkedHashMap<>(selected));
        this.optionalIncluded = Collections.unmodifiableSet(new LinkedHashSet<>(optionalIncluded));
        this.optionalMissing  = Collections.unmodifiableSet(new LinkedHashSet<>(optionalMissing));
        this.decisions        = decisions;
        this.duration         = Objects.requireNonNull(duration, "duration");
    }

    public Map<String, Provider> selected()      { return selected; }

    /** Опциональные зависимости, которые удалось включить. */
    public Set<String> optionalIncluded()        { return optionalIncluded; }

    /** Опциональные зависимости, которые не удалось (или не захотелось) включить. */
    public Set<String> optionalMissing()         { return optionalMissing; }

    /** Количество принятых решений до получения ответа (для метрик/диагностики). */
    public int decisions()                       { return decisions; }

    public Duration duration()                   { return duration; }

    /**
     * Ищет провайдер, отвечающий за id — либо как родной, либо через
     * {@link Provider#provides()}. Используйте для lookup’а по алиасам.
     */
    public Optional<Provider> findByQuery(String queryId) {
        Objects.requireNonNull(queryId, "queryId");
        Provider direct = selected.get(queryId);
        if (direct != null) return Optional.of(direct);
        for (Provider p : selected.values()) {
            if (p.offers(queryId)) return Optional.of(p);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "Resolution[selected=" + selected.size()
                + ", optional+=" + optionalIncluded.size()
                + ", optional-=" + optionalMissing.size()
                + ", decisions=" + decisions
                + ", duration=" + duration.toMillis() + "ms]";
    }
}
