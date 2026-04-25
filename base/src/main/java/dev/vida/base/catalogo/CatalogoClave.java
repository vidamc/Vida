/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.catalogo;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;

/**
 * Типизированный ключ регистрации в {@link Catalogo}.
 *
 * <p>Состоит из двух частей:
 * <ul>
 *   <li>{@link #reestroId() reestroId} — идентификатор самого реестра
 *       ({@code "vida:block"}, {@code "ejemplo:herramienta"});</li>
 *   <li>{@link #id() id} — идентификатор записи внутри реестра
 *       ({@code "ejemplo:espada_sagrada"}).</li>
 * </ul>
 *
 * <p>Параметр типа {@code T} удерживает тип значения: это даёт
 * компилятор-таймовую проверку, что один и тот же {@link CatalogoClave}
 * используется только с совместимыми значениями.
 *
 * <p>Equals/hashCode учитывают только пару идентификаторов — тип параметра
 * стирается в JVM, что соответствует стандартной Java-семантике.
 */
@ApiStatus.Stable
public final class CatalogoClave<T> {

    private final Identifier reestroId;
    private final Identifier id;

    private CatalogoClave(Identifier reestroId, Identifier id) {
        this.reestroId = Objects.requireNonNull(reestroId, "reestroId");
        this.id = Objects.requireNonNull(id, "id");
    }

    public Identifier reestroId() { return reestroId; }
    public Identifier id()        { return id; }

    public static <T> CatalogoClave<T> de(Identifier reestroId, Identifier id) {
        return new CatalogoClave<>(reestroId, id);
    }

    public static <T> CatalogoClave<T> de(String reestroId, String id) {
        return new CatalogoClave<>(Identifier.parse(reestroId), Identifier.parse(id));
    }

    /** Shortcut: один и тот же namespace для записи. */
    public static <T> CatalogoClave<T> de(String reestroId, String namespace, String path) {
        return new CatalogoClave<>(Identifier.parse(reestroId), Identifier.of(namespace, path));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CatalogoClave<?> other)) return false;
        return reestroId.equals(other.reestroId) && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return reestroId.hashCode() * 31 + id.hashCode();
    }

    @Override
    public String toString() {
        return reestroId + " @ " + id;
    }
}
