/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ResultTest {

    @Test
    void okCarriesValue() {
        Result<Integer, String> r = Result.ok(42);
        assertThat(r.isOk()).isTrue();
        assertThat(r.isErr()).isFalse();
        assertThat(r.unwrap()).isEqualTo(42);
        assertThat(r.ok()).contains(42);
        assertThat(r.err()).isEmpty();
        assertThatThrownBy(r::unwrapErr).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void errCarriesError() {
        Result<Integer, String> r = Result.err("boom");
        assertThat(r.isErr()).isTrue();
        assertThat(r.unwrapErr()).isEqualTo("boom");
        assertThat(r.err()).contains("boom");
        assertThat(r.ok()).isEmpty();
        assertThatThrownBy(r::unwrap)
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void mapTransformsValue() {
        Result<Integer, String> r = Result.<Integer, String>ok(3).map(i -> i * 2);
        assertThat(r.unwrap()).isEqualTo(6);

        Result<Integer, String> err = Result.<Integer, String>err("x").map(i -> i * 2);
        assertThat(err.unwrapErr()).isEqualTo("x");
    }

    @Test
    void mapErrTransformsError() {
        Result<Integer, Integer> r = Result.<Integer, String>err("x").mapErr(String::length);
        assertThat(r.unwrapErr()).isEqualTo(1);
    }

    @Test
    void flatMapChains() {
        Result<Integer, String> r = Result.<Integer, String>ok(5)
                .flatMap(i -> i > 0 ? Result.ok(i + 1) : Result.err("neg"));
        assertThat(r.unwrap()).isEqualTo(6);
    }

    @Test
    void orElseReturnsFallbackForErr() {
        Result<Integer, String> err = Result.err("x");
        assertThat(err.orElse(99)).isEqualTo(99);
        assertThat(err.orElseGet(e -> e.length())).isEqualTo(1);
    }

    @Test
    void orElseThrowMapsError() {
        assertThatThrownBy(() -> Result.<Integer, String>err("boom")
                .orElseThrow(IllegalStateException::new))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void peekAndPeekErrAreConditional() {
        AtomicReference<Integer> seenOk = new AtomicReference<>();
        AtomicReference<String> seenErr = new AtomicReference<>();

        Result.<Integer, String>ok(10).peek(seenOk::set).peekErr(seenErr::set);
        assertThat(seenOk.get()).isEqualTo(10);
        assertThat(seenErr.get()).isNull();

        Result.<Integer, String>err("x").peek(seenOk::set).peekErr(seenErr::set);
        assertThat(seenErr.get()).isEqualTo("x");
        assertThat(seenOk.get()).isEqualTo(10); // не изменилось
    }

    @Test
    void catchingCapturesException() {
        Result<Integer, Throwable> r = Result.catching(
                () -> { throw new IllegalStateException("kaboom"); },
                t -> t);
        assertThat(r.isErr()).isTrue();
        assertThat(r.unwrapErr()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ofNullable() {
        assertThat(Result.ofNullable("x", () -> "missing").unwrap()).isEqualTo("x");
        assertThat(Result.ofNullable(null, () -> "missing").unwrapErr()).isEqualTo("missing");
    }
}
