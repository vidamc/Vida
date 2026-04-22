/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.manifest;

import dev.vida.core.ApiStatus;
import dev.vida.core.Version;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Распарсенный {@code vida.mod.json}.
 *
 * <p>Обязательные поля: {@code schema}, {@code id}, {@code version}, {@code name}.
 * Всё остальное опционально.
 *
 * <p>Схема версионируется через поле {@code schema}; в рамках данной реализации
 * поддерживается {@value #SCHEMA_V1}.
 */
@ApiStatus.Stable
public record ModManifest(
        int schema,
        String id,
        Version version,
        String name,
        Optional<String> description,
        List<ModAuthor> authors,
        Optional<String> license,
        ModEntrypoints entrypoints,
        ModDependencies dependencies,
        VifadaConfig vifada,
        List<String> puertas,
        List<String> modules,
        List<String> incompatibilities,
        Map<String, Object> custom) {

    public static final int SCHEMA_V1 = 1;

    /** Регулярка допустимого mod id (подмножество Identifier, без {@code ':'}). */
    public static final String ID_PATTERN = "[a-z0-9_.-]+";

    public ModManifest {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(license, "license");
        Objects.requireNonNull(entrypoints, "entrypoints");
        Objects.requireNonNull(dependencies, "dependencies");
        Objects.requireNonNull(vifada, "vifada");

        if (id.isBlank()) {
            throw new IllegalArgumentException("mod id must not be blank");
        }
        if (!id.matches(ID_PATTERN)) {
            throw new IllegalArgumentException(
                    "mod id '" + id + "' does not match " + ID_PATTERN);
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("mod name must not be blank");
        }

        authors           = List.copyOf(Objects.requireNonNull(authors, "authors"));
        puertas           = List.copyOf(Objects.requireNonNull(puertas, "puertas"));
        modules           = List.copyOf(Objects.requireNonNull(modules, "modules"));
        incompatibilities = List.copyOf(Objects.requireNonNull(incompatibilities, "incompatibilities"));
        custom            = Map.copyOf(Objects.requireNonNull(custom, "custom"));
    }

    /** Создаёт билдер с обязательными полями. */
    public static Builder builder(String id, Version version, String name) {
        return new Builder(id, version, name);
    }

    @Override
    public String toString() {
        return "ModManifest[" + id + " " + version + "]";
    }

    // =====================================================================
    //                                 BUILDER
    // =====================================================================

    public static final class Builder {
        private int schema = SCHEMA_V1;
        private final String id;
        private final Version version;
        private final String name;
        private Optional<String> description = Optional.empty();
        private List<ModAuthor> authors = List.of();
        private Optional<String> license = Optional.empty();
        private ModEntrypoints entrypoints = ModEntrypoints.EMPTY;
        private ModDependencies dependencies = ModDependencies.EMPTY;
        private VifadaConfig vifada = VifadaConfig.EMPTY;
        private List<String> puertas = List.of();
        private List<String> modules = List.of();
        private List<String> incompatibilities = List.of();
        private Map<String, Object> custom = Map.of();

        private Builder(String id, Version version, String name) {
            this.id = id;
            this.version = version;
            this.name = name;
        }

        public Builder schema(int v) { this.schema = v; return this; }
        public Builder description(String s) { this.description = Optional.ofNullable(s); return this; }
        public Builder authors(List<ModAuthor> v) { this.authors = v; return this; }
        public Builder license(String s) { this.license = Optional.ofNullable(s); return this; }
        public Builder entrypoints(ModEntrypoints v) { this.entrypoints = v; return this; }
        public Builder dependencies(ModDependencies v) { this.dependencies = v; return this; }
        public Builder vifada(VifadaConfig v) { this.vifada = v; return this; }
        public Builder puertas(List<String> v) { this.puertas = v; return this; }
        public Builder modules(List<String> v) { this.modules = v; return this; }
        public Builder incompatibilities(List<String> v) { this.incompatibilities = v; return this; }
        public Builder custom(Map<String, Object> v) { this.custom = v; return this; }

        public ModManifest build() {
            return new ModManifest(
                    schema, id, version, name,
                    description, authors, license,
                    entrypoints, dependencies, vifada,
                    puertas, modules, incompatibilities, custom);
        }
    }
}
