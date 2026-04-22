/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery;

import dev.vida.core.ApiStatus;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Результат скана {@link ModScanner}.
 *
 * <p>Хранит список top-level кандидатов, плоский обход всех кандидатов
 * включая вложенные, список ошибок и длительность скана. Иммутабелен.
 */
@ApiStatus.Stable
public final class DiscoveryReport {

    private final List<ModCandidate> topLevel;
    private final List<ModCandidate> flattened;
    private final List<DiscoveryError> errors;
    private final Duration duration;

    public DiscoveryReport(
            List<ModCandidate> topLevel,
            List<DiscoveryError> errors,
            Duration duration) {
        Objects.requireNonNull(topLevel, "topLevel");
        Objects.requireNonNull(errors, "errors");
        Objects.requireNonNull(duration, "duration");
        this.topLevel = List.copyOf(topLevel);
        this.errors = List.copyOf(errors);
        this.duration = duration;

        List<ModCandidate> flat = new ArrayList<>(topLevel.size() * 2);
        for (ModCandidate c : this.topLevel) {
            appendFlat(c, flat);
        }
        this.flattened = List.copyOf(flat);
    }

    /** Кандидаты, найденные непосредственно в сканируемой директории. */
    public List<ModCandidate> topLevel() {
        return topLevel;
    }

    /** Все кандидаты, включая вложенные jar-ы, в порядке обхода. */
    public List<ModCandidate> all() {
        return flattened;
    }

    /** Ошибки скана. Пустой список — полный успех. */
    public List<DiscoveryError> errors() {
        return errors;
    }

    /** Длительность скана (для метрик запуска). */
    public Duration duration() {
        return duration;
    }

    /** Были ли зафиксированы ошибки. */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /** Количество кандидатов всех уровней. */
    public int size() {
        return flattened.size();
    }

    @Override
    public String toString() {
        return "DiscoveryReport{top=" + topLevel.size()
                + ", all=" + flattened.size()
                + ", errors=" + errors.size()
                + ", duration=" + duration + "}";
    }

    private static void appendFlat(ModCandidate c, List<ModCandidate> out) {
        out.add(c);
        for (ModCandidate child : c.nested()) {
            appendFlat(child, out);
        }
    }
}
