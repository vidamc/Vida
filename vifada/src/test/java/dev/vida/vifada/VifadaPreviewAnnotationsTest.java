/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.vifada;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;

class VifadaPreviewAnnotationsTest {

    @Test
    void vifada_multi_define_defaults() throws Exception {
        assertThat(VifadaMulti.class.getMethod("requireTargets").getDefaultValue()).isEqualTo(true);
        assertThat(VifadaMulti.class.getMethod("methods").getReturnType()).isEqualTo(String[].class);
    }

    @Test
    void vifada_local_target_es_parametro() {
        Target target = VifadaLocal.class.getAnnotation(Target.class);
        assertThat(target).isNotNull();
        assertThat(target.value()).containsExactly(ElementType.PARAMETER);
        assertThat(VifadaLocal.class.getDeclaredMethods())
                .extracting(m -> m.getName())
                .containsExactlyInAnyOrder("ordinal", "descriptor", "mutable");
    }
}
