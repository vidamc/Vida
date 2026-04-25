/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base;

import dev.vida.core.ApiStatus;
import dev.vida.manifest.ModManifest;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Иммутабельный снимок <strong>разрешённого</strong> набора модов после бутстрапа Vida.
 *
 * <p>Заполняется загрузчиком один раз при успешном {@code BootSequence}; мододелу не
 * нужен модуль {@code :loader} — достаточно {@code vida-base} + {@code vida-manifest}.
 *
 * <p>Если Vida ещё не выполнила бутстрап, {@link #vista()} возвращает пустой список.
 */
@ApiStatus.Stable
public final class ModulosInstaladosGlobal {

    private static volatile List<ModManifest> instalados = List.of();

    private ModulosInstaladosGlobal() {}

    /**
     * Устанавливает список разрешённых модов (вызывается только из загрузчика).
     */
    public static void instalar(List<ModManifest> mods) {
        Objects.requireNonNull(mods, "mods");
        instalados = List.copyOf(mods);
    }

    /** Снимок манифестов в порядке резолюции. */
    public static List<ModManifest> vista() {
        return instalados;
    }

    /** Поиск по {@code vida.mod.json:id}. */
    public static Optional<ModManifest> buscarPorId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        for (ModManifest m : instalados) {
            if (id.equals(m.id())) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    /** Только для тестов: сброс глобального состояния. */
    @ApiStatus.Internal
    public static void resetForTests() {
        instalados = List.of();
    }
}
