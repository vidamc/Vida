/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.base.LatidoGlobal;
import dev.vida.base.catalogo.CatalogoManejador;
import dev.vida.base.latidos.LatidoBus;
import dev.vida.cartografia.MappingTree;
import dev.vida.core.ApiStatus;
import dev.vida.fuente.FuenteContenidoMod;
import dev.vida.loader.profile.PlatformProfileDescriptor;
import dev.vida.manifest.ModManifest;
import java.lang.instrument.Instrumentation;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Иммутабельное рантайм-состояние после успешного бутстрапа Vida.
 *
 * <p>Создаётся {@link VidaBoot} или {@link VidaPremain} и хранится в
 * {@link VidaRuntime#current()} для доступа извне.
 */
@ApiStatus.Stable
public final class VidaEnvironment {

    private final BootOptions options;
    private final Instant startedAt;
    private final List<ModManifest> resolvedMods;
    private final MorphIndex morphs;
    private final JuegoLoader juegoLoader;
    private final Map<String, ModLoader> modLoaders;
    private final Map<String, FuenteContenidoMod> fuenteDataDriven;
    private final VidaClassTransformer transformer;
    private final Instrumentation instrumentation; // nullable
    private final LatidoBus latidos;
    private final CatalogoManejador catalogos;
    /** Mojang {@code client_mappings.txt} разобранный в дерево; пусто если файла не было. */
    private final MappingTree clientMappings;
    /** Активный профиль платформы, если был запрошен и успешно загружен. */
    private final Optional<PlatformProfileDescriptor> platformProfile;

    private VidaEnvironment(Builder b) {
        this.options      = Objects.requireNonNull(b.options, "options");
        this.startedAt    = Objects.requireNonNull(b.startedAt, "startedAt");
        this.resolvedMods = List.copyOf(b.resolvedMods);
        this.morphs       = Objects.requireNonNull(b.morphs, "morphs");
        this.juegoLoader  = Objects.requireNonNull(b.juegoLoader, "juegoLoader");
        this.modLoaders   = Collections.unmodifiableMap(new LinkedHashMap<>(b.modLoaders));
        this.fuenteDataDriven = Collections.unmodifiableMap(new LinkedHashMap<>(b.fuenteDataDriven));
        this.transformer  = Objects.requireNonNull(b.transformer, "transformer");
        this.instrumentation = b.instrumentation;
        this.latidos      = Objects.requireNonNull(b.latidos, "latidos");
        this.catalogos    = Objects.requireNonNull(b.catalogos, "catalogos");
        this.clientMappings = b.clientMappings;
        this.platformProfile = b.platformProfile == null ? Optional.empty() : b.platformProfile;
    }

    public BootOptions options()               { return options; }
    public Instant startedAt()                 { return startedAt; }
    public List<ModManifest> resolvedMods()    { return resolvedMods; }
    public MorphIndex morphs()                 { return morphs; }
    public JuegoLoader juegoLoader()           { return juegoLoader; }
    public Map<String, ModLoader> modLoaders() { return modLoaders; }
    public Map<String, FuenteContenidoMod> fuenteDataDriven() { return fuenteDataDriven; }
    public VidaClassTransformer transformer()  { return transformer; }
    public Instrumentation instrumentation()   { return instrumentation; }
    public LatidoBus latidos()                 { return latidos; }
    public CatalogoManejador catalogos()       { return catalogos; }

    /** Дерево маппингов клиента (obf ↔ mojmap), если загрузилось из {@code versions/&lt;mc&gt;/client_mappings.txt}. */
    public Optional<MappingTree> clientMappings() {
        return Optional.ofNullable(clientMappings);
    }

    /**
     * Дескриптор профиля платформы (Cartografía / Vifada / мост), если он был выбран при бутстрапе.
     */
    public Optional<PlatformProfileDescriptor> platformProfile() {
        return platformProfile;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        BootOptions options;
        Instant startedAt = Instant.now();
        List<ModManifest> resolvedMods = List.of();
        MorphIndex morphs = MorphIndex.empty();
        JuegoLoader juegoLoader;
        final Map<String, ModLoader> modLoaders = new LinkedHashMap<>();
        final Map<String, FuenteContenidoMod> fuenteDataDriven = new LinkedHashMap<>();
        VidaClassTransformer transformer;
        Instrumentation instrumentation;
        LatidoBus latidos;
        CatalogoManejador catalogos;
        MappingTree clientMappings;
        Optional<PlatformProfileDescriptor> platformProfile = Optional.empty();

        public Builder options(BootOptions v)      { this.options = v; return this; }
        public Builder startedAt(Instant v)        { this.startedAt = v; return this; }
        public Builder resolvedMods(List<ModManifest> v) { this.resolvedMods = v; return this; }
        public Builder morphs(MorphIndex v)        { this.morphs = v; return this; }
        public Builder juegoLoader(JuegoLoader v)  { this.juegoLoader = v; return this; }
        public Builder addModLoader(String id, ModLoader l) { modLoaders.put(id, l); return this; }
        public Builder addFuenteDataDriven(String modId, FuenteContenidoMod contenido) {
            fuenteDataDriven.put(modId, contenido);
            return this;
        }
        public Builder transformer(VidaClassTransformer v)  { this.transformer = v; return this; }
        public Builder instrumentation(Instrumentation v)   { this.instrumentation = v; return this; }
        public Builder latidos(LatidoBus v)                 { this.latidos = v; return this; }
        public Builder catalogos(CatalogoManejador v)       { this.catalogos = v; return this; }
        public Builder clientMappings(MappingTree v)      { this.clientMappings = v; return this; }

        public Builder platformProfile(Optional<PlatformProfileDescriptor> v) {
            this.platformProfile = v == null ? Optional.empty() : v;
            return this;
        }

        public VidaEnvironment build() { return new VidaEnvironment(this); }
    }
}
