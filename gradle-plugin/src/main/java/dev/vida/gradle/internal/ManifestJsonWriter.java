/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.gradle.internal;

import dev.vida.gradle.ModInfoSpec;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Мини-генератор {@code vida.mod.json} из {@link ModInfoSpec}.
 *
 * <p>Пишем руками — структура фиксированная, добавлять зависимость на
 * внешний JSON-сериализатор ради нескольких полей смысла нет. Вывод —
 * стабильный (алфавитный порядок ключей), с правильным экранированием,
 * без BOM, UTF-8 при сохранении.
 *
 * <p>Совместим с грамматикой vida-JSON ({@code :manifest}) — тот же
 * парсер успешно прочитает то, что мы пишем.
 */
public final class ManifestJsonWriter {

    private ManifestJsonWriter() {}

    /** Главная точка входа. */
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

        StringBuilder b = new StringBuilder(512);
        b.append("{\n");
        writeNumField(b, "schema", schema, true);
        writeStrField(b, "id", id, true);
        writeStrField(b, "version", version, true);
        writeStrField(b, "name", name, true);
        if (description != null && !description.isEmpty()) {
            writeStrField(b, "description", description, true);
        }
        if (license != null && !license.isEmpty()) {
            writeStrField(b, "license", license, true);
        }
        if (authors != null && !authors.isEmpty()) {
            writeArrayField(b, "authors", authors, true);
        }
        if (entrypoint != null && !entrypoint.isEmpty()) {
            // entrypoints.main — массив строк (FQN классов)
            b.append("  \"entrypoints\": {\n");
            b.append("    \"main\": [");
            writeJsonString(b, entrypoint);
            b.append("]\n  },\n");
        }
        boolean hasRequired = dependencies != null && !dependencies.isEmpty();
        boolean hasOptional = optionalDependencies != null && !optionalDependencies.isEmpty();
        if (hasRequired || hasOptional) {
            b.append("  \"dependencies\": {\n");
            boolean first = true;
            if (hasRequired) {
                b.append("    \"required\": ");
                writeObject(b, new TreeMap<>(dependencies), 4);
                first = false;
            }
            if (hasOptional) {
                if (!first) b.append(",\n");
                b.append("    \"optional\": ");
                writeObject(b, new TreeMap<>(optionalDependencies), 4);
            }
            b.append("\n  },\n");
        }
        if (incompatibilities != null && !incompatibilities.isEmpty()) {
            writeArrayField(b, "incompatibilities", incompatibilities, true);
        }
        if (puertas != null && !puertas.isEmpty()) {
            writeArrayField(b, "puertas", puertas, true);
        }
        if (escultores != null && !escultores.isEmpty()) {
            writeArrayField(b, "escultores", escultores, true);
        }
        // Удалим последнюю ","
        if (b.charAt(b.length() - 2) == ',') {
            b.delete(b.length() - 2, b.length() - 1);
        }
        b.append("}\n");
        return b.toString();
    }

    // ============================================================= helpers

    private static void writeStrField(StringBuilder b, String key, String val, boolean comma) {
        b.append("  \"").append(key).append("\": ");
        writeJsonString(b, val);
        if (comma) b.append(',');
        b.append('\n');
    }

    private static void writeNumField(StringBuilder b, String key, int val, boolean comma) {
        b.append("  \"").append(key).append("\": ").append(val);
        if (comma) b.append(',');
        b.append('\n');
    }

    private static void writeArrayField(StringBuilder b, String key, List<String> items, boolean comma) {
        b.append("  \"").append(key).append("\": ");
        writeArray(b, items, 2);
        if (comma) b.append(',');
        b.append('\n');
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
            if (i < items.size() - 1) b.append(',');
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
        int i = 0, n = map.size();
        for (Map.Entry<String, String> e : map.entrySet()) {
            pad(b, indent + 2);
            writeJsonString(b, e.getKey());
            b.append(": ");
            writeJsonString(b, e.getValue());
            if (++i < n) b.append(',');
            b.append('\n');
        }
        pad(b, indent);
        b.append('}');
    }

    private static void pad(StringBuilder b, int indent) {
        for (int i = 0; i < indent; i++) b.append(' ');
    }

    private static void writeJsonString(StringBuilder b, String s) {
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\b': b.append("\\b");  break;
                case '\f': b.append("\\f");  break;
                case '\n': b.append("\\n");  break;
                case '\r': b.append("\\r");  break;
                case '\t': b.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
            }
        }
        b.append('"');
    }
}
