/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import dev.vida.core.ApiStatus;
import dev.vida.core.Version;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Мод, который резолвер может выбрать как решение для какого-то id.
 *
 * <p>Provider иммутабелен. Построить экземпляр проще всего через
 * {@link #builder(String, Version)}.
 *
 * <h2>Aliases / {@code provides}</h2>
 * Список {@link #provides()} позволяет моду заявлять «я реализую такой-то
 * другой id» — например, форк или замена оригинала. Резолвер будет
 * рассматривать такого провайдера кандидатом для всех объявленных в
 * {@code provides} имён наравне с его собственным {@link #id()}.
 *
 * <h2>Payload</h2>
 * {@link #attachment()} — произвольный пользовательский объект (например,
 * {@code ModCandidate} из {@code :discovery}), который резолвер не трогает,
 * а возвращает в {@link Resolution#selected()} по запросу.
 */
@ApiStatus.Stable
public final class Provider {

    private final String id;
    private final Version version;
    private final List<String> provides;
    private final List<Dependency> dependencies;
    private final Object attachment;

    private Provider(String id, Version version, List<String> provides,
                     List<Dependency> dependencies, Object attachment) {
        this.id = id;
        this.version = version;
        this.provides = provides;
        this.dependencies = dependencies;
        this.attachment = attachment;
    }

    public String id()                     { return id; }
    public Version version()                { return version; }
    public List<String> provides()          { return provides; }
    public List<Dependency> dependencies()  { return dependencies; }

    /**
     * Пользовательская полезная нагрузка; {@code null}, если не задана.
     * Обычно — {@code ModCandidate} или аналогичный контекст.
     */
    public Object attachment() { return attachment; }

    /**
     * Возвращает {@code true}, если этот провайдер предоставляет указанный id —
     * совпадением {@link #id()} либо одним из {@link #provides()}.
     */
    public boolean offers(String queryId) {
        if (id.equals(queryId)) return true;
        for (int i = 0, n = provides.size(); i < n; i++) {
            if (provides.get(i).equals(queryId)) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "Provider[" + id + " " + version + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Provider other)) return false;
        return id.equals(other.id) && version.equals(other.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    // =====================================================================
    //                                BUILDER
    // =====================================================================

    public static Builder builder(String id, Version version) {
        return new Builder(id, version);
    }

    public static final class Builder {

        private final String id;
        private final Version version;
        private final Set<String> provides = new LinkedHashSet<>();
        private final List<Dependency> dependencies = new ArrayList<>();
        private Object attachment;

        private Builder(String id, Version version) {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(version, "version");
            if (id.isBlank()) {
                throw new IllegalArgumentException("id must not be blank");
            }
            this.id = id;
            this.version = version;
        }

        public Builder provides(String alias) {
            Objects.requireNonNull(alias, "alias");
            if (alias.isBlank()) {
                throw new IllegalArgumentException("alias must not be blank");
            }
            if (alias.equals(id)) {
                // Собственный id и так подходит, запись в provides — шумиха.
                return this;
            }
            provides.add(alias);
            return this;
        }

        public Builder provides(Iterable<String> aliases) {
            Objects.requireNonNull(aliases, "aliases");
            for (String a : aliases) provides(a);
            return this;
        }

        public Builder dependency(Dependency dep) {
            Objects.requireNonNull(dep, "dep");
            dependencies.add(dep);
            return this;
        }

        public Builder dependencies(Iterable<Dependency> deps) {
            Objects.requireNonNull(deps, "deps");
            for (Dependency d : deps) dependency(d);
            return this;
        }

        public Builder attachment(Object attachment) {
            this.attachment = attachment;
            return this;
        }

        public Provider build() {
            return new Provider(
                    id, version,
                    provides.isEmpty() ? List.of() : List.copyOf(provides),
                    dependencies.isEmpty() ? List.of() : List.copyOf(dependencies),
                    attachment);
        }
    }

    /** Утилита, возвращающая unmodifiable view списка — для внутренних сравнений. */
    static List<String> emptyList() { return Collections.emptyList(); }
}
