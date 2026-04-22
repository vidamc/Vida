/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.render;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Иммутабельный snapshot texture-atlas.
 */
@ApiStatus.Preview("render")
public final class TextureAtlas {

    private final Identifier texturaMissing;
    private final Set<Identifier> texturas;

    private TextureAtlas(Builder builder) {
        this.texturaMissing = Objects.requireNonNull(builder.texturaMissing, "texturaMissing");
        LinkedHashSet<Identifier> ids = new LinkedHashSet<>(builder.texturas);
        ids.add(texturaMissing);
        this.texturas = Set.copyOf(ids);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Identifier texturaMissing() {
        return texturaMissing;
    }

    public boolean contiene(Identifier textura) {
        return texturas.contains(textura);
    }

    public Identifier resolver(Identifier textura) {
        Objects.requireNonNull(textura, "textura");
        return contiene(textura) ? textura : texturaMissing;
    }

    public Set<Identifier> texturas() {
        return texturas;
    }

    public static final class Builder {
        private Identifier texturaMissing = ModeloBloque.TEXTURA_MISSING;
        private final LinkedHashSet<Identifier> texturas = new LinkedHashSet<>();

        private Builder() {}

        public Builder texturaMissing(Identifier texturaMissing) {
            this.texturaMissing = Objects.requireNonNull(texturaMissing, "texturaMissing");
            return this;
        }

        public Builder registrar(Identifier textura) {
            texturas.add(Objects.requireNonNull(textura, "textura"));
            return this;
        }

        public Builder registrarTodas(Collection<Identifier> texturas) {
            Objects.requireNonNull(texturas, "texturas");
            for (Identifier textura : texturas) {
                registrar(textura);
            }
            return this;
        }

        public TextureAtlas build() {
            return new TextureAtlas(this);
        }
    }
}
