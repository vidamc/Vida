/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mundo;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Objects;

/**
 * Value-type измерения мира.
 */
@ApiStatus.Stable
public record Dimension(
        Identifier id,
        boolean natural,
        boolean permiteCama,
        boolean techoFijo) {

    public static final Dimension OVERWORLD =
            new Dimension(Identifier.of("minecraft", "overworld"), true, true, false);

    public static final Dimension NETHER =
            new Dimension(Identifier.of("minecraft", "the_nether"), false, false, true);

    public static final Dimension END =
            new Dimension(Identifier.of("minecraft", "the_end"), false, false, false);

    public Dimension {
        Objects.requireNonNull(id, "id");
    }

    public static Dimension de(Identifier id, boolean natural, boolean permiteCama, boolean techoFijo) {
        return new Dimension(id, natural, permiteCama, techoFijo);
    }

    public static Dimension de(String id, boolean natural, boolean permiteCama, boolean techoFijo) {
        return new Dimension(Identifier.parse(id), natural, permiteCama, techoFijo);
    }

    /**
     * Типичные вертикальные границы для профиля Vanilla 1.21.x по {@link #id}.
     *
     * <p>Для пользовательских измерений без известного профиля возвращаются те же границы, что и у
     * {@link #OVERWORLD} — наиболее широкий типичный столбец; рантайм может уточнять через
     * переопределение {@link Mundo#limitesVerticales()}.
     */
    public LimitesVerticales limitesVerticalesPredeterminados() {
        if (id.equals(NETHER.id())) {
            return LimitesVerticales.netherVanilla121();
        }
        if (id.equals(OVERWORLD.id())) {
            return LimitesVerticales.overworldVanilla121();
        }
        if (id.equals(END.id())) {
            return LimitesVerticales.endVanilla121();
        }
        return LimitesVerticales.overworldVanilla121();
    }
}
