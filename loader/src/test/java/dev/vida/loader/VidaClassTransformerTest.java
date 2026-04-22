/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import static org.assertj.core.api.Assertions.*;

import dev.vida.vifada.MorphSource;
import java.lang.instrument.IllegalClassFormatException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class VidaClassTransformerTest {

    @Test
    void passthrough_when_no_morphs() throws IllegalClassFormatException {
        byte[] target = TestSupport.buildPlainClass("x/Y");
        MorphIndex idx = MorphIndex.empty();
        VidaClassTransformer t = new VidaClassTransformer(idx, e -> {});

        byte[] out = t.transform(null, "x/Y", null, null, target);
        assertThat(out).isNull(); // null == "не трогали" в контракте ClassFileTransformer
        // Намеренно: на no-morph hot-path мы не считаем классы, чтобы не было
        // CAS-contention в многопоточной загрузке классов. Счётчик остаётся
        // на нуле — проверяем, что реальной работы не было.
        assertThat(t.transformedCount()).isZero();
        assertThat(t.errorCount()).isZero();
    }

    @Test
    void transforms_when_morph_present() throws IllegalClassFormatException {
        byte[] target = TestSupport.buildSimpleTarget("x/Foo");
        byte[] morph = TestSupport.buildHeadInjectMorph("x/FooMorph", "x.Foo");

        MorphIndex idx = MorphIndex.builder()
                .add("x/Foo", new MorphSource("x/FooMorph", morph))
                .build();
        VidaClassTransformer t = new VidaClassTransformer(idx, e -> {});

        byte[] out = t.transform(null, "x/Foo", null, null, target);
        assertThat(out).isNotNull();
        assertThat(out).isNotEqualTo(target);   // байткод реально изменился
        assertThat(t.transformedCount()).isEqualTo(1);
        assertThat(t.errorCount()).isZero();
    }

    @Test
    void skips_own_classes() throws IllegalClassFormatException {
        byte[] dummy = TestSupport.buildPlainClass("dev/vida/loader/Something");
        MorphIndex idx = MorphIndex.builder()
                .add("dev/vida/loader/Something",
                        new MorphSource("x/M", new byte[] {0})).build();
        VidaClassTransformer t = new VidaClassTransformer(idx, e -> {});

        byte[] out = t.transform(null, "dev/vida/loader/Something", null, null, dummy);
        assertThat(out).isNull();
        // Собственные классы не трансформируются (иначе class-circularity)
        // и не увеличивают счётчик skipped — это быстрый путь.
        assertThat(t.transformedCount()).isZero();
        assertThat(t.errorCount()).isZero();
    }

    @Test
    void records_errors_for_broken_morphs() throws IllegalClassFormatException {
        byte[] target = TestSupport.buildSimpleTarget("x/Z");
        byte[] badMorph = TestSupport.buildPlainClass("x/NotAMorph"); // без @VifadaMorph

        MorphIndex idx = MorphIndex.builder()
                .add("x/Z", new MorphSource("x/NotAMorph", badMorph))
                .build();
        List<LoaderError> collected = new ArrayList<>();
        VidaClassTransformer t = new VidaClassTransformer(idx, collected::add);

        byte[] out = t.transform(null, "x/Z", null, null, target);
        // Байты возвращены, ошибки собраны.
        assertThat(out).isNotNull();
        assertThat(collected).hasSize(1);
        assertThat(collected.get(0)).isInstanceOf(LoaderError.VifadaFailed.class);
        assertThat(t.errorCount()).isEqualTo(1);
    }
}
