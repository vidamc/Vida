/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.fuente;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Ссылки на loot-таблицы и блоки, извлечённые из одного JSON под {@code worldgen/**}.
 */
@ApiStatus.Stable
public record FuenteWorldgenHuella(String pathZip, List<Identifier> refsLoot, List<Identifier> refsBloque) {

    public FuenteWorldgenHuella {
        Objects.requireNonNull(pathZip, "pathZip");
        refsLoot = List.copyOf(Objects.requireNonNull(refsLoot, "refsLoot"));
        refsBloque = List.copyOf(Objects.requireNonNull(refsBloque, "refsBloque"));
    }

    /**
     * Упорядоченные списки без дубликатов (стабильный порядок для снимков и тестов).
     */
    public static FuenteWorldgenHuella dedup(String pathZip, Set<Identifier> loot, Set<Identifier> bloques) {
        List<Identifier> l = new ArrayList<>(loot);
        List<Identifier> b = new ArrayList<>(bloques);
        l.sort(Comparator.comparing(Identifier::toString));
        b.sort(Comparator.comparing(Identifier::toString));
        return new FuenteWorldgenHuella(pathZip, l, b);
    }
}
