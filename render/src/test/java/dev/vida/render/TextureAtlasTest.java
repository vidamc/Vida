/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.render;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.core.Identifier;
import org.junit.jupiter.api.Test;

class TextureAtlasTest {

    @Test
    void missing_texture_esta_disponible_siempre() {
        TextureAtlas atlas = TextureAtlas.builder().build();

        assertThat(atlas.contiene(ModeloBloque.TEXTURA_MISSING)).isTrue();
        assertThat(atlas.resolver(Identifier.of("demo", "foo/bar")))
                .isEqualTo(ModeloBloque.TEXTURA_MISSING);
    }

    @Test
    void builder_registra_texturas_y_resuelve_correctamente() {
        Identifier t1 = Identifier.of("demo", "textures/a");
        Identifier t2 = Identifier.of("demo", "textures/b");
        TextureAtlas atlas = TextureAtlas.builder()
                .texturaMissing(t1)
                .registrar(t2)
                .build();

        assertThat(atlas.texturaMissing()).isEqualTo(t1);
        assertThat(atlas.contiene(t2)).isTrue();
        assertThat(atlas.resolver(t2)).isEqualTo(t2);
        assertThat(atlas.resolver(Identifier.of("demo", "textures/c"))).isEqualTo(t1);
    }
}
