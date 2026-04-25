/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.render;

import dev.vida.bloque.Bloque;
import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import dev.vida.entidad.Entidad;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Центральная точка render API: связывает модели блоков/сущностей, atlas и shader hooks.
 *
 * <p>Безопасный fallback:
 * <ul>
 *   <li>блок без регистрации модели рендерится как {@code cube + missing texture};</li>
 *   <li>сущность без регистрации — {@code entity/simple + missing texture};</li>
 *   <li>неизвестная текстура автоматически заменяется на atlas missing-texture.</li>
 * </ul>
 */
@ApiStatus.Stable
public final class RenderPipeline {

    private final TextureAtlas atlas;
    private final Map<Identifier, ModeloBloque> modelosBloque = new LinkedHashMap<>();
    private final Map<Identifier, ModeloEntidad> modelosEntidad = new LinkedHashMap<>();
    private final EnumMap<ShaderHook.Etapa, List<ShaderHook>> hooks =
            new EnumMap<>(ShaderHook.Etapa.class);

    public RenderPipeline(TextureAtlas atlas) {
        this.atlas = Objects.requireNonNull(atlas, "atlas");
        for (ShaderHook.Etapa etapa : ShaderHook.Etapa.values()) {
            hooks.put(etapa, new ArrayList<>());
        }
    }

    public static RenderPipeline conAtlasDefault() {
        return new RenderPipeline(TextureAtlas.builder().build());
    }

    public TextureAtlas atlas() {
        return atlas;
    }

    public void registrarModeloBloque(Identifier bloqueId, ModeloBloque modelo) {
        modelosBloque.put(Objects.requireNonNull(bloqueId, "bloqueId"),
                normalizar(Objects.requireNonNull(modelo, "modelo")));
    }

    public void registrarModeloEntidad(Identifier entidadId, ModeloEntidad modelo) {
        modelosEntidad.put(Objects.requireNonNull(entidadId, "entidadId"),
                normalizar(Objects.requireNonNull(modelo, "modelo")));
    }

    public ModeloBloque resolverModelo(Bloque bloque) {
        Objects.requireNonNull(bloque, "bloque");
        ModeloBloque modelo = modelosBloque.get(bloque.id());
        if (modelo == null) {
            return new ModeloBloque(
                    ModeloBloque.GEOMETRIA_CUBO,
                    atlas.texturaMissing());
        }
        return modelo;
    }

    public ModeloEntidad resolverModelo(Entidad entidad) {
        Objects.requireNonNull(entidad, "entidad");
        ModeloEntidad modelo = modelosEntidad.get(entidad.id());
        if (modelo == null) {
            return new ModeloEntidad(
                    ModeloEntidad.MALLA_SIMPLE,
                    atlas.texturaMissing());
        }
        return modelo;
    }

    public void registrarHook(ShaderHook.Etapa etapa, ShaderHook hook) {
        hooks.get(Objects.requireNonNull(etapa, "etapa"))
                .add(Objects.requireNonNull(hook, "hook"));
    }

    public int ejecutarHooks(ShaderHook.Etapa etapa, long frameId, long tiempoNanos) {
        List<ShaderHook> porEtapa = hooks.get(Objects.requireNonNull(etapa, "etapa"));
        ContextoShader contexto = new ContextoShader(frameId, tiempoNanos, etapa);
        for (ShaderHook hook : porEtapa) {
            hook.aplicar(contexto);
        }
        return porEtapa.size();
    }

    public Map<Identifier, ModeloBloque> modelosBloque() {
        return Map.copyOf(modelosBloque);
    }

    public Map<Identifier, ModeloEntidad> modelosEntidad() {
        return Map.copyOf(modelosEntidad);
    }

    private ModeloBloque normalizar(ModeloBloque modelo) {
        Identifier textura = atlas.resolver(modelo.texturaPrincipal());
        return textura.equals(modelo.texturaPrincipal())
                ? modelo
                : new ModeloBloque(modelo.geometria(), textura);
    }

    private ModeloEntidad normalizar(ModeloEntidad modelo) {
        Identifier textura = atlas.resolver(modelo.texturaPrincipal());
        return textura.equals(modelo.texturaPrincipal())
                ? modelo
                : new ModeloEntidad(modelo.malla(), textura);
    }
}
