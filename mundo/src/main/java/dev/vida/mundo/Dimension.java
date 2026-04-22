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
@ApiStatus.Preview("mundo")
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
}
