/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class CallbackInfoTest {

    @Test
    void fresh_instance_is_not_cancelled() {
        CallbackInfo ci = new CallbackInfo("tick");
        assertThat(ci.isCancelled()).isFalse();
        assertThat(ci.methodName()).isEqualTo("tick");
    }

    @Test
    void cancel_marks_cancelled() {
        CallbackInfo ci = new CallbackInfo("x");
        ci.cancel();
        assertThat(ci.isCancelled()).isTrue();
    }

    @Test
    void cancel_is_idempotent() {
        CallbackInfo ci = new CallbackInfo("x");
        ci.cancel(); ci.cancel(); ci.cancel();
        assertThat(ci.isCancelled()).isTrue();
    }
}
