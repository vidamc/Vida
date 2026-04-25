/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.fuente;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.List;
import java.util.Objects;

/**
 * Упрощённое представление таблицы лута datapack: item-id из пулов и вложенные ссылки на другие
 * loot-таблицы ({@code type: minecraft:loot_table}).
 */
@ApiStatus.Stable
public record FuenteLootTable(
        Identifier id,
        List<Identifier> itemsExtraidos,
        List<Identifier> tablasReferenciadas) {

    public FuenteLootTable {
        Objects.requireNonNull(id, "id");
        itemsExtraidos = List.copyOf(Objects.requireNonNull(itemsExtraidos, "itemsExtraidos"));
        tablasReferenciadas = List.copyOf(Objects.requireNonNull(tablasReferenciadas, "tablasReferenciadas"));
    }
}
