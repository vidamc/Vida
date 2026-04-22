/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.resolver;

import dev.vida.core.Version;
import dev.vida.core.VersionRange;
import java.util.ArrayList;
import java.util.List;

/**
 * Мини-DSL для краткой постройки провайдеров и универсов в тестах.
 */
final class Providers {

    private Providers() {}

    static Provider p(String id, String version, Dep... deps) {
        Provider.Builder b = Provider.builder(id, Version.parse(version));
        for (Dep d : deps) b.dependency(d.toDependency());
        return b.build();
    }

    static Provider pp(String id, String version, List<String> provides, Dep... deps) {
        Provider.Builder b = Provider.builder(id, Version.parse(version));
        b.provides(provides);
        for (Dep d : deps) b.dependency(d.toDependency());
        return b.build();
    }

    static Universe uni(Provider... ps) {
        List<Provider> list = new ArrayList<>(ps.length);
        for (Provider p : ps) list.add(p);
        return Universe.of(list);
    }

    static Dep req(String id, String range) {
        return new Dep(id, range, DependencyKind.REQUIRED);
    }

    static Dep opt(String id, String range) {
        return new Dep(id, range, DependencyKind.OPTIONAL);
    }

    static Dep inc(String id, String range) {
        return new Dep(id, range, DependencyKind.INCOMPATIBLE);
    }

    record Dep(String id, String range, DependencyKind kind) {
        Dependency toDependency() {
            return new Dependency(id, VersionRange.parse(range), kind);
        }
    }
}
