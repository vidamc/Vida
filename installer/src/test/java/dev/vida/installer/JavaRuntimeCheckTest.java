/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class JavaRuntimeCheckTest {

    @Test
    void java_17_fails() {
        var r = JavaRuntimeCheck.check(Runtime.Version.parse("17.0.9"));
        assertThat(r.ok()).isFalse();
        assertThat(r.major()).isEqualTo(17);
        assertThat(r.message()).contains("17").contains("21");
    }

    @Test
    void java_21_passes() {
        var r = JavaRuntimeCheck.check(Runtime.Version.parse("21.0.4"));
        assertThat(r.ok()).isTrue();
        assertThat(r.major()).isEqualTo(21);
    }

    @Test
    void java_25_passes() {
        var r = JavaRuntimeCheck.check(Runtime.Version.parse("25-ea+3"));
        assertThat(r.ok()).isTrue();
        assertThat(r.major()).isEqualTo(25);
    }

    @Test
    void check_current_works_and_is_ok_on_21_plus() {
        var r = JavaRuntimeCheck.checkCurrent();
        // Тесты запускаются на JDK 21 (toolchain проекта); результат должен быть ок.
        assertThat(r.ok()).as("running on %s", r.version()).isTrue();
    }
}
