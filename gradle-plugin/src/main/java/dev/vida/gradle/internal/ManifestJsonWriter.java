/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle.internal;

import dev.vida.gradle.ModInfoSpec;
import dev.vida.manifest.VifadaConfig;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Мини-генератор {@code vida.mod.json} из {@link ModInfoSpec}.
 *
 * <p>Пишем руками — структура фиксированная, добавлять зависимость на
 * внешний JSON-сериализатор ради нескольких полей смысла нет. Вывод —
 * стабильный (предсказуемый порядок ключей верхнего уровня), с правильным экранированием,
 * без BOM, UTF-8 при сохранении.
 *
 * <p>Совместим с грамматикой vida-JSON ({@code :manifest}) — тот же
 * парсер успешно прочитает то, что мы пишем.
 */
public final class ManifestJsonWriter {

    private ManifestJsonWriter() {}

    /**
     * Полное описание манифеста для генерации (все опциональные секции — пустые коллекции).
     */
    public static final class Draft {
        private final int schema;
        private final String id;
        private final String version;
        private final String name;
        private final String description;
        private final String license;
        private final List<String> authors;
        private final List<String> entryPreLaunch;
        private final List<String> entryMain;
        private final List<String> entryClient;
        private final List<String> entryServer;
        private final Map<String, String> dependencies;
        private final Map<String, String> optionalDependencies;
        private final List<String> incompatibilities;
        private final List<String> puertas;
        private final List<String> escultores;
        private final List<String> vifadaPackages;
        private final String vifadaConfig;
        private final int vifadaPriority;
        private final List<String> modules;

        private Draft(Builder b) {
            this.schema = b.schema;
            this.id = Objects.requireNonNull(b.id, "id");
            this.version = Objects.requireNonNull(b.version, "version");
            this.name = Objects.requireNonNull(b.name, "name");
            this.description = b.description == null ? "" : b.description;
            this.license = b.license == null ? "" : b.license;
            this.authors = List.copyOf(b.authors);
            this.entryPreLaunch = List.copyOf(b.entryPreLaunch);
            this.entryMain = List.copyOf(b.entryMain);
            this.entryClient = List.copyOf(b.entryClient);
            this.entryServer = List.copyOf(b.entryServer);
            this.dependencies = Map.copyOf(b.dependencies);
            this.optionalDependencies = Map.copyOf(b.optionalDependencies);
            this.incompatibilities = List.copyOf(b.incompatibilities);
            this.puertas = List.copyOf(b.puertas);
            this.escultores = List.copyOf(b.escultores);
            this.vifadaPackages = List.copyOf(b.vifadaPackages);
            this.vifadaConfig = b.vifadaConfig == null ? "" : b.vifadaConfig;
            this.vifadaPriority = b.vifadaPriority;
            this.modules = List.copyOf(b.modules);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private int schema = 1;
            private String id;
            private String version;
            private String name;
            private String description = "";
            private String license = "";
            private List<String> authors = List.of();
            private List<String> entryPreLaunch = List.of();
            private List<String> entryMain = List.of();
            private List<String> entryClient = List.of();
            private List<String> entryServer = List.of();
            private Map<String, String> dependencies = Map.of();
            private Map<String, String> optionalDependencies = Map.of();
            private List<String> incompatibilities = List.of();
            private List<String> puertas = List.of();
            private List<String> escultores = List.of();
            private List<String> vifadaPackages = List.of();
            private String vifadaConfig = "";
            private int vifadaPriority = VifadaConfig.DEFAULT_PRIORITY;
            private List<String> modules = List.of();

            public Builder schema(int schema) {
                this.schema = schema;
                return this;
            }

            public Builder id(String id) {
                this.id = id;
                return this;
            }

            public Builder version(String version) {
                this.version = version;
                return this;
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            public Builder license(String license) {
                this.license = license;
                return this;
            }

            public Builder authors(List<String> authors) {
                this.authors = authors == null ? List.of() : authors;
                return this;
            }

            public Builder entryPreLaunch(List<String> v) {
                this.entryPreLaunch = v == null ? List.of() : v;
                return this;
            }

            public Builder entryMain(List<String> v) {
                this.entryMain = v == null ? List.of() : v;
                return this;
            }

            public Builder entryClient(List<String> v) {
                this.entryClient = v == null ? List.of() : v;
                return this;
            }

            public Builder entryServer(List<String> v) {
                this.entryServer = v == null ? List.of() : v;
                return this;
            }

            public Builder dependencies(Map<String, String> dependencies) {
                this.dependencies = dependencies == null ? Map.of() : dependencies;
                return this;
            }

            public Builder optionalDependencies(Map<String, String> optionalDependencies) {
                this.optionalDependencies = optionalDependencies == null ? Map.of() : optionalDependencies;
                return this;
            }

            public Builder incompatibilities(List<String> incompatibilities) {
                this.incompatibilities = incompatibilities == null ? List.of() : incompatibilities;
                return this;
            }

            public Builder puertas(List<String> puertas) {
                this.puertas = puertas == null ? List.of() : puertas;
                return this;
            }

            public Builder escultores(List<String> escultores) {
                this.escultores = escultores == null ? List.of() : escultores;
                return this;
            }

            public Builder vifadaPackages(List<String> vifadaPackages) {
                this.vifadaPackages = vifadaPackages == null ? List.of() : vifadaPackages;
                return this;
            }

            public Builder vifadaConfig(String vifadaConfig) {
                this.vifadaConfig = vifadaConfig;
                return this;
            }

            public Builder vifadaPriority(int vifadaPriority) {
                this.vifadaPriority = vifadaPriority;
                return this;
            }

            public Builder modules(List<String> modules) {
                this.modules = modules == null ? List.of() : modules;
                return this;
            }

            public Draft build() {
                return new Draft(this);
            }
        }
    }

    /** Генерирует JSON из полного черновика. */
    public static String toJson(Draft draft) {
        StringBuilder b = new StringBuilder(768);
        b.append("{\n");
        appendScalar(b, "schema", draft.schema, true);
        appendStr(b, "id", draft.id, true);
        appendStr(b, "version", draft.version, true);
        appendStr(b, "name", draft.name, true);
        if (!draft.description.isEmpty()) {
            appendStr(b, "description", draft.description, true);
        }
        if (!draft.license.isEmpty()) {
            appendStr(b, "license", draft.license, true);
        }
        if (!draft.authors.isEmpty()) {
            appendStringArrayTop(b, "authors", draft.authors, true);
        }
        if (!draft.entryPreLaunch.isEmpty()
                || !draft.entryMain.isEmpty()
                || !draft.entryClient.isEmpty()
                || !draft.entryServer.isEmpty()) {
            appendEntrypoints(b, draft, true);
        }
        boolean hasRequired = !draft.dependencies.isEmpty();
        boolean hasOptional = !draft.optionalDependencies.isEmpty();
        if (hasRequired || hasOptional) {
            b.append("  \"dependencies\": {\n");
            boolean first = true;
            if (hasRequired) {
                b.append("    \"required\": ");
                writeObject(b, new TreeMap<>(draft.dependencies), 4);
                first = false;
            }
            if (hasOptional) {
                if (!first) {
                    b.append(",\n");
                }
                b.append("    \"optional\": ");
                writeObject(b, new TreeMap<>(draft.optionalDependencies), 4);
            }
            b.append("\n  },\n");
        }
        if (!draft.incompatibilities.isEmpty()) {
            appendStringArrayTop(b, "incompatibilities", draft.incompatibilities, true);
        }
        if (!draft.modules.isEmpty()) {
            appendStringArrayTop(b, "modules", draft.modules, true);
        }
        if (!draft.puertas.isEmpty()) {
            appendStringArrayTop(b, "puertas", draft.puertas, true);
        }
        if (!draft.escultores.isEmpty()) {
            appendStringArrayTop(b, "escultores", draft.escultores, true);
        }
        if (!draft.vifadaPackages.isEmpty()
                || !draft.vifadaConfig.isBlank()
                || draft.vifadaPriority != VifadaConfig.DEFAULT_PRIORITY) {
            appendVifada(b, draft, true);
        }
        stripTrailingCommaBeforeClose(b);
        b.append("}\n");
        return b.toString();
    }

    /**
     * Совместимость со старым API: один {@code entrypoint} попадает в {@code entrypoints.main}.
     */
    public static String toJson(
            int schema,
            String id,
            String version,
            String name,
            String description,
            String license,
            String entrypoint,
            List<String> authors,
            List<String> puertas,
            List<String> escultores,
            Map<String, String> dependencies,
            Map<String, String> optionalDependencies,
            List<String> incompatibilities) {

        List<String> main = List.of();
        if (entrypoint != null && !entrypoint.isEmpty()) {
            main = List.of(entrypoint);
        }
        return toJson(Draft.builder()
                .schema(schema)
                .id(id)
                .version(version)
                .name(name)
                .description(description == null ? "" : description)
                .license(license == null ? "" : license)
                .authors(authors == null ? List.of() : authors)
                .entryMain(main)
                .puertas(puertas == null ? List.of() : puertas)
                .escultores(escultores == null ? List.of() : escultores)
                .dependencies(dependencies == null ? Map.of() : dependencies)
                .optionalDependencies(optionalDependencies == null ? Map.of() : optionalDependencies)
                .incompatibilities(incompatibilities == null ? List.of() : incompatibilities)
                .build());
    }

    private static void appendEntrypoints(StringBuilder b, Draft d, boolean commaAfter) {
        b.append("  \"entrypoints\": {\n");
        boolean first = true;
        if (!d.entryClient.isEmpty()) {
            appendStringArrayInner(b, "client", d.entryClient, first);
            first = false;
        }
        if (!d.entryMain.isEmpty()) {
            appendStringArrayInner(b, "main", d.entryMain, first);
            first = false;
        }
        if (!d.entryPreLaunch.isEmpty()) {
            appendStringArrayInner(b, "preLaunch", d.entryPreLaunch, first);
            first = false;
        }
        if (!d.entryServer.isEmpty()) {
            appendStringArrayInner(b, "server", d.entryServer, first);
            first = false;
        }
        b.append("  }");
        if (commaAfter) {
            b.append(',');
        }
        b.append('\n');
    }

    private static void appendStringArrayInner(
            StringBuilder b, String key, List<String> items, boolean firstField) {
        if (!firstField) {
            b.append(",\n");
        }
        b.append("    \"").append(key).append("\": ");
        writeArray(b, items, 4);
    }

    private static void appendVifada(StringBuilder b, Draft d, boolean commaAfter) {
        b.append("  \"vifada\": {\n");
        boolean first = true;
        if (!d.vifadaConfig.isBlank()) {
            appendStrIndent(b, "config", d.vifadaConfig, 4, first);
            first = false;
        }
        if (!d.vifadaPackages.isEmpty()) {
            if (!first) {
                b.append(",\n");
            }
            b.append("    \"packages\": ");
            writeArray(b, d.vifadaPackages, 4);
            first = false;
        }
        if (d.vifadaPriority != VifadaConfig.DEFAULT_PRIORITY) {
            if (!first) {
                b.append(",\n");
            }
            b.append("    \"priority\": ").append(d.vifadaPriority);
        }
        b.append("\n  }");
        if (commaAfter) {
            b.append(',');
        }
        b.append('\n');
    }

    private static void appendStrIndent(
            StringBuilder b, String key, String val, int spaces, boolean firstField) {
        if (!firstField) {
            b.append(",\n");
        }
        pad(b, spaces);
        b.append('"').append(key).append("\": ");
        writeJsonString(b, val);
    }

    private static void appendScalar(StringBuilder b, String key, int val, boolean comma) {
        b.append("  \"").append(key).append("\": ").append(val);
        if (comma) {
            b.append(',');
        }
        b.append('\n');
    }

    private static void appendStr(StringBuilder b, String key, String val, boolean comma) {
        b.append("  \"").append(key).append("\": ");
        writeJsonString(b, val);
        if (comma) {
            b.append(',');
        }
        b.append('\n');
    }

    private static void appendStringArrayTop(StringBuilder b, String key, List<String> items, boolean comma) {
        b.append("  \"").append(key).append("\": ");
        writeArray(b, items, 2);
        if (comma) {
            b.append(',');
        }
        b.append('\n');
    }

    private static void stripTrailingCommaBeforeClose(StringBuilder b) {
        int len = b.length();
        if (len >= 2 && b.charAt(len - 1) == '\n' && b.charAt(len - 2) == ',') {
            b.deleteCharAt(len - 2);
        }
    }

    private static void writeArray(StringBuilder b, List<String> items, int indent) {
        if (items.isEmpty()) {
            b.append("[]");
            return;
        }
        b.append("[\n");
        for (int i = 0; i < items.size(); i++) {
            pad(b, indent + 2);
            writeJsonString(b, items.get(i));
            if (i < items.size() - 1) {
                b.append(',');
            }
            b.append('\n');
        }
        pad(b, indent);
        b.append(']');
    }

    private static void writeObject(StringBuilder b, Map<String, String> map, int indent) {
        if (map.isEmpty()) {
            b.append("{}");
            return;
        }
        b.append("{\n");
        int i = 0;
        int n = map.size();
        for (Map.Entry<String, String> e : map.entrySet()) {
            pad(b, indent + 2);
            writeJsonString(b, e.getKey());
            b.append(": ");
            writeJsonString(b, e.getValue());
            if (++i < n) {
                b.append(',');
            }
            b.append('\n');
        }
        pad(b, indent);
        b.append('}');
    }

    private static void pad(StringBuilder b, int indent) {
        for (int i = 0; i < indent; i++) {
            b.append(' ');
        }
    }

    private static void writeJsonString(StringBuilder b, String s) {
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\b' -> b.append("\\b");
                case '\f' -> b.append("\\f");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        b.append('"');
    }

    /**
     * Собирает {@link Draft} из DSL: объединяет устаревшее одиночное {@code entrypoint}
     * со списками {@code entrypoints.main}.
     */
    public static Draft draftFromModSpec(
            int schema,
            String id,
            String version,
            String displayName,
            String description,
            String license,
            String legacyEntrypoint,
            List<String> entryPreLaunch,
            List<String> entryMainExtra,
            List<String> entryClient,
            List<String> entryServer,
            List<String> authors,
            List<String> puertas,
            List<String> escultores,
            List<String> vifadaPackages,
            String vifadaConfig,
            int vifadaPriority,
            List<String> modules,
            Map<String, String> dependencies,
            Map<String, String> optionalDependencies,
            List<String> incompatibilities) {

        LinkedHashSet<String> main = new LinkedHashSet<>();
        if (legacyEntrypoint != null && !legacyEntrypoint.isBlank()) {
            main.add(legacyEntrypoint.trim());
        }
        if (entryMainExtra != null) {
            for (String s : entryMainExtra) {
                if (s != null && !s.isBlank()) {
                    main.add(s.trim());
                }
            }
        }
        return Draft.builder()
                .schema(schema)
                .id(id)
                .version(version)
                .name(displayName)
                .description(description == null ? "" : description)
                .license(license == null ? "" : license)
                .authors(authors == null ? List.of() : authors)
                .entryPreLaunch(entryPreLaunch == null ? List.of() : entryPreLaunch)
                .entryMain(new ArrayList<>(main))
                .entryClient(entryClient == null ? List.of() : entryClient)
                .entryServer(entryServer == null ? List.of() : entryServer)
                .dependencies(dependencies == null ? Map.of() : dependencies)
                .optionalDependencies(optionalDependencies == null ? Map.of() : optionalDependencies)
                .incompatibilities(incompatibilities == null ? List.of() : incompatibilities)
                .puertas(puertas == null ? List.of() : puertas)
                .escultores(escultores == null ? List.of() : escultores)
                .vifadaPackages(vifadaPackages == null ? List.of() : vifadaPackages)
                .vifadaConfig(vifadaConfig == null ? "" : vifadaConfig)
                .vifadaPriority(vifadaPriority)
                .modules(modules == null ? List.of() : modules)
                .build();
    }
}
