/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.render;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.bloque.Bloque;
import dev.vida.bloque.MaterialBloque;
import dev.vida.bloque.PropiedadesBloque;
import dev.vida.core.Identifier;
import dev.vida.entidad.Entidad;
import dev.vida.entidad.PropiedadesEntidad;
import dev.vida.entidad.TipoEntidad;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RenderPipelineTest {

    @Test
    void usa_fallback_seguro_si_no_hay_modelos() {
        RenderPipeline pipeline = RenderPipeline.conAtlasDefault();
        Bloque bloque = new Bloque(
                Identifier.of("demo", "piedra"),
                PropiedadesBloque.con(MaterialBloque.PIEDRA).construir());
        Entidad entidad = new Entidad(
                Identifier.of("demo", "zorro"),
                TipoEntidad.CRIATURA,
                PropiedadesEntidad.con().construir());

        ModeloBloque modeloBloque = pipeline.resolverModelo(bloque);
        ModeloEntidad modeloEntidad = pipeline.resolverModelo(entidad);

        assertThat(modeloBloque.geometria()).isEqualTo(ModeloBloque.GEOMETRIA_CUBO);
        assertThat(modeloBloque.texturaPrincipal()).isEqualTo(ModeloBloque.TEXTURA_MISSING);
        assertThat(modeloEntidad.malla()).isEqualTo(ModeloEntidad.MALLA_SIMPLE);
        assertThat(modeloEntidad.texturaPrincipal()).isEqualTo(ModeloEntidad.TEXTURA_MISSING);
    }

    @Test
    void normaliza_texturas_desconocidas_a_missing() {
        Identifier texturaValida = Identifier.of("demo", "textures/bloque/piedra");
        TextureAtlas atlas = TextureAtlas.builder().registrar(texturaValida).build();
        RenderPipeline pipeline = new RenderPipeline(atlas);

        Identifier bloqueId = Identifier.of("demo", "piedra");
        Identifier entidadId = Identifier.of("demo", "ciervo");
        pipeline.registrarModeloBloque(
                bloqueId,
                new ModeloBloque(
                        Identifier.of("demo", "model/bloque/piedra"),
                        Identifier.of("demo", "textures/bloque/desconocida")));
        pipeline.registrarModeloEntidad(
                entidadId,
                new ModeloEntidad(
                        Identifier.of("demo", "model/entidad/ciervo"),
                        texturaValida));

        Bloque bloque = new Bloque(bloqueId, PropiedadesBloque.con().construir());
        Entidad entidad = new Entidad(entidadId, TipoEntidad.CRIATURA, PropiedadesEntidad.con().construir());

        assertThat(pipeline.resolverModelo(bloque).texturaPrincipal()).isEqualTo(ModeloBloque.TEXTURA_MISSING);
        assertThat(pipeline.resolverModelo(entidad).texturaPrincipal()).isEqualTo(texturaValida);
    }

    @Test
    void ejecuta_hooks_por_etapa_en_orden_de_registro() {
        RenderPipeline pipeline = RenderPipeline.conAtlasDefault();
        List<String> orden = new ArrayList<>();

        pipeline.registrarHook(ShaderHook.Etapa.ANTES_MUNDO, ctx -> orden.add("a:" + ctx.frameId()));
        pipeline.registrarHook(ShaderHook.Etapa.ANTES_MUNDO, ctx -> orden.add("b:" + ctx.frameId()));
        pipeline.registrarHook(ShaderHook.Etapa.HUD, ctx -> orden.add("hud"));

        int ejecutados = pipeline.ejecutarHooks(ShaderHook.Etapa.ANTES_MUNDO, 7L, 11L);

        assertThat(ejecutados).isEqualTo(2);
        assertThat(orden).containsExactly("a:7", "b:7");
    }
}
