/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.latidos;

import static org.assertj.core.api.Assertions.*;

import dev.vida.core.Identifier;
import org.junit.jupiter.api.Test;

final class LatidoTest {

    record A() {}
    record B() {}

    @Test
    void equals_requires_id_and_class() {
        Latido<A> a1 = Latido.de("t:a", A.class);
        Latido<A> a2 = Latido.de(Identifier.of("t", "a"), A.class);
        Latido<B> b  = Latido.de("t:a", B.class);

        assertThat(a1).isEqualTo(a2);
        assertThat(a1).isNotEqualTo(b);
        assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
    }

    @Test
    void toString_is_readable() {
        Latido<A> a = Latido.de("test:foo", A.class);
        assertThat(a.toString()).contains("test:foo").contains("A");
    }
}
