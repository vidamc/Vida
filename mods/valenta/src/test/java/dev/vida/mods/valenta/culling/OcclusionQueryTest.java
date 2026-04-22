/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.mods.valenta.culling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.vida.mods.valenta.core.GlFunctions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OcclusionQueryTest {

    private GlFunctions.Noop gl;
    private OcclusionQuery query;

    @BeforeEach
    void setUp() {
        gl = new GlFunctions.Noop();
        query = new OcclusionQuery(gl);
    }

    @AfterEach
    void tearDown() {
        if (query != null) query.close();
    }

    @Test
    void initialVisibility_isTrue() {
        assertThat(query.isVisible()).isTrue();
    }

    @Test
    void beginEnd_cycle() {
        query.begin();
        assertThat(query.isActive()).isTrue();
        query.end();
        assertThat(query.isActive()).isFalse();
    }

    @Test
    void doubleBegin_throws() {
        query.begin();
        assertThatThrownBy(query::begin).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void endWithoutBegin_throws() {
        assertThatThrownBy(query::end).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void afterClose_methodsThrow() {
        query.close();
        assertThatThrownBy(query::begin).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void close_whileActive_safeCleanup() {
        query.begin();
        query.close();
        assertThatThrownBy(query::begin).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void isVisible_afterQuery_reflectsResult() {
        query.begin();
        query.end();
        boolean vis = query.isVisible();
        assertThat(vis).isFalse();
    }
}
