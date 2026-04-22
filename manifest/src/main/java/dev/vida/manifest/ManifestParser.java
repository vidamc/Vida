/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.manifest;

import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import dev.vida.core.Version;
import dev.vida.core.VersionRange;
import dev.vida.manifest.json.JsonException;
import dev.vida.manifest.json.JsonReader;
import dev.vida.manifest.json.JsonToken;
import dev.vida.manifest.json.VidaJson;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Парсер {@code vida.mod.json}.
 *
 * <p>Использует {@link JsonReader} под капотом и возвращает типизированный
 * {@link ModManifest} либо {@link ManifestError} — без исключений в обычных
 * ошибочных сценариях.
 */
@ApiStatus.Stable
public final class ManifestParser {

    public static final int SUPPORTED_SCHEMA_MAX = ModManifest.SCHEMA_V1;

    private ManifestParser() {}

    public static Result<ModManifest, ManifestError> parse(String json) {
        try (JsonReader in = VidaJson.reader(json)) {
            return parseDocument(in);
        } catch (JsonException ex) {
            return Result.err(new ManifestError.SyntaxError(ex.getMessage(), ex.line(), ex.column()));
        }
    }

    public static Result<ModManifest, ManifestError> parse(Reader reader) {
        try (JsonReader in = VidaJson.reader(reader)) {
            return parseDocument(in);
        } catch (JsonException ex) {
            return Result.err(new ManifestError.SyntaxError(ex.getMessage(), ex.line(), ex.column()));
        }
    }

    // =====================================================================
    //                              PARSING
    // =====================================================================

    private static Result<ModManifest, ManifestError> parseDocument(JsonReader in) {
        // Проверка, что верхний уровень — объект.
        if (in.peek() != JsonToken.BEGIN_OBJECT) {
            return Result.err(new ManifestError.WrongType("<root>", "object", tokenName(in.peek())));
        }
        in.beginObject();

        Integer schema = null;
        String id = null;
        Version version = null;
        String name = null;
        Optional<String> description = Optional.empty();
        List<ModAuthor> authors = List.of();
        Optional<String> license = Optional.empty();
        ModEntrypoints entrypoints = ModEntrypoints.EMPTY;
        ModDependencies dependencies = ModDependencies.EMPTY;
        VifadaConfig vifada = VifadaConfig.EMPTY;
        List<String> puertas = List.of();
        List<String> modules = List.of();
        List<String> incompatibilities = List.of();
        Map<String, Object> custom = Map.of();

        try {
            while (in.hasNext()) {
                String field = in.nextName();
                switch (field) {
                    case "schema" -> schema = in.nextInt();
                    case "id" -> id = in.nextString();
                    case "version" -> {
                        String rawVer = in.nextString();
                        version = Version.tryParse(rawVer).orElse(null);
                        if (version == null) {
                            return Result.err(new ManifestError.InvalidValue("version",
                                    "'" + rawVer + "' is not a valid SemVer 2.0.0 version"));
                        }
                    }
                    case "name" -> name = in.nextString();
                    case "description" -> description = Optional.of(in.nextString());
                    case "authors" -> {
                        var res = parseAuthors(in);
                        if (res.isErr()) return Result.err(res.unwrapErr());
                        authors = res.unwrap();
                    }
                    case "license" -> license = Optional.of(in.nextString());
                    case "entrypoints" -> {
                        var res = parseEntrypoints(in);
                        if (res.isErr()) return Result.err(res.unwrapErr());
                        entrypoints = res.unwrap();
                    }
                    case "dependencies" -> {
                        var res = parseDependencies(in);
                        if (res.isErr()) return Result.err(res.unwrapErr());
                        dependencies = res.unwrap();
                    }
                    case "incompatibilities" -> {
                        // Список id, с которыми мод несовместим (без ранжей).
                        // Ранжи — в dependencies.incompatibilities.
                        var res = readStringArray(in, "incompatibilities");
                        if (res.isErr()) return Result.err(res.unwrapErr());
                        incompatibilities = res.unwrap();
                    }
                    case "vifada" -> {
                        var res = parseVifada(in);
                        if (res.isErr()) return Result.err(res.unwrapErr());
                        vifada = res.unwrap();
                    }
                    case "puertas" -> {
                        var res = readStringArray(in, "puertas");
                        if (res.isErr()) return Result.err(res.unwrapErr());
                        puertas = res.unwrap();
                    }
                    case "modules" -> {
                        var res = readStringArray(in, "modules");
                        if (res.isErr()) return Result.err(res.unwrapErr());
                        modules = res.unwrap();
                    }
                    case "custom" -> {
                        var res = readGenericObject(in);
                        if (res.isErr()) return Result.err(res.unwrapErr());
                        custom = res.unwrap();
                    }
                    default -> in.skipValue(); // forward-compat: игнорируем неизвестные поля
                }
            }
            in.endObject();
            if (in.peek() != JsonToken.EOF) {
                return Result.err(new ManifestError.SyntaxError(
                        "trailing content after root object", in.line(), in.column()));
            }
        } catch (JsonException ex) {
            return Result.err(new ManifestError.SyntaxError(ex.getMessage(), ex.line(), ex.column()));
        }

        // Валидация обязательных полей
        if (schema == null) return Result.err(new ManifestError.MissingField("schema"));
        if (schema < 1) return Result.err(new ManifestError.InvalidValue("schema", "must be >= 1"));
        if (schema > SUPPORTED_SCHEMA_MAX) {
            return Result.err(new ManifestError.UnsupportedSchema(schema, SUPPORTED_SCHEMA_MAX));
        }
        if (id == null) return Result.err(new ManifestError.MissingField("id"));
        if (version == null) return Result.err(new ManifestError.MissingField("version"));
        if (name == null) return Result.err(new ManifestError.MissingField("name"));

        try {
            ModManifest manifest = ModManifest.builder(id, version, name)
                    .schema(schema)
                    .description(description.orElse(null))
                    .authors(authors)
                    .license(license.orElse(null))
                    .entrypoints(entrypoints)
                    .dependencies(dependencies)
                    .vifada(vifada)
                    .puertas(puertas)
                    .modules(modules)
                    .incompatibilities(incompatibilities)
                    .custom(custom)
                    .build();
            return Result.ok(manifest);
        } catch (IllegalArgumentException ex) {
            return Result.err(new ManifestError.InvalidValue("<root>", ex.getMessage()));
        }
    }

    // ------------------------------------------------------------- authors

    private static Result<List<ModAuthor>, ManifestError> parseAuthors(JsonReader in) {
        if (in.peek() != JsonToken.BEGIN_ARRAY) {
            return Result.err(new ManifestError.WrongType("authors", "array", tokenName(in.peek())));
        }
        in.beginArray();
        List<ModAuthor> list = new ArrayList<>();
        while (in.hasNext()) {
            JsonToken t = in.peek();
            if (t == JsonToken.STRING) {
                list.add(ModAuthor.of(in.nextString()));
            } else if (t == JsonToken.BEGIN_OBJECT) {
                in.beginObject();
                String name = null;
                String contact = null;
                while (in.hasNext()) {
                    String field = in.nextName();
                    switch (field) {
                        case "name" -> name = in.nextString();
                        case "contact" -> contact = in.nextString();
                        default -> in.skipValue();
                    }
                }
                in.endObject();
                if (name == null || name.isBlank()) {
                    return Result.err(new ManifestError.MissingField("authors[].name"));
                }
                list.add(ModAuthor.of(name, contact));
            } else {
                return Result.err(new ManifestError.WrongType(
                        "authors[]", "string or object", tokenName(t)));
            }
        }
        in.endArray();
        return Result.ok(List.copyOf(list));
    }

    // -------------------------------------------------------- entrypoints

    private static Result<ModEntrypoints, ManifestError> parseEntrypoints(JsonReader in) {
        if (in.peek() != JsonToken.BEGIN_OBJECT) {
            return Result.err(new ManifestError.WrongType("entrypoints", "object", tokenName(in.peek())));
        }
        in.beginObject();
        List<String> preLaunch = List.of();
        List<String> main = List.of();
        List<String> client = List.of();
        List<String> server = List.of();
        while (in.hasNext()) {
            String field = in.nextName();
            Result<List<String>, ManifestError> arr = readStringArray(in, "entrypoints." + field);
            if (arr.isErr()) return Result.err(arr.unwrapErr());
            switch (field) {
                case "preLaunch" -> preLaunch = arr.unwrap();
                case "main"      -> main      = arr.unwrap();
                case "client"    -> client    = arr.unwrap();
                case "server"    -> server    = arr.unwrap();
                default          -> { /* forward-compat */ }
            }
        }
        in.endObject();
        return Result.ok(new ModEntrypoints(preLaunch, main, client, server));
    }

    // -------------------------------------------------------- dependencies

    private static Result<ModDependencies, ManifestError> parseDependencies(JsonReader in) {
        if (in.peek() != JsonToken.BEGIN_OBJECT) {
            return Result.err(new ManifestError.WrongType("dependencies", "object", tokenName(in.peek())));
        }
        in.beginObject();
        Map<String, VersionRange> required = Map.of();
        Map<String, VersionRange> optional = Map.of();
        Map<String, VersionRange> incompat = Map.of();
        while (in.hasNext()) {
            String field = in.nextName();
            Result<Map<String, VersionRange>, ManifestError> sub =
                    readDependencyMap(in, "dependencies." + field);
            if (sub.isErr()) return Result.err(sub.unwrapErr());
            switch (field) {
                case "required"         -> required = sub.unwrap();
                case "optional"         -> optional = sub.unwrap();
                case "incompatibilities" -> incompat = sub.unwrap();
                default                  -> { /* forward-compat */ }
            }
        }
        in.endObject();
        return Result.ok(new ModDependencies(required, optional, incompat));
    }

    private static Result<Map<String, VersionRange>, ManifestError> readDependencyMap(JsonReader in, String path) {
        if (in.peek() != JsonToken.BEGIN_OBJECT) {
            return Result.err(new ManifestError.WrongType(path, "object", tokenName(in.peek())));
        }
        in.beginObject();
        Map<String, VersionRange> out = new LinkedHashMap<>();
        while (in.hasNext()) {
            String key = in.nextName();
            String rawRange = in.nextString();
            Optional<VersionRange> parsed = VersionRange.tryParse(rawRange);
            if (parsed.isEmpty()) {
                return Result.err(new ManifestError.InvalidValue(path + "." + key,
                        "'" + rawRange + "' is not a valid version range"));
            }
            out.put(key, parsed.get());
        }
        in.endObject();
        return Result.ok(Map.copyOf(out));
    }

    // --------------------------------------------------------------- vifada

    private static Result<VifadaConfig, ManifestError> parseVifada(JsonReader in) {
        if (in.peek() != JsonToken.BEGIN_OBJECT) {
            return Result.err(new ManifestError.WrongType("vifada", "object", tokenName(in.peek())));
        }
        in.beginObject();
        List<String> packages = List.of();
        Optional<String> config = Optional.empty();
        int priority = VifadaConfig.DEFAULT_PRIORITY;
        while (in.hasNext()) {
            String field = in.nextName();
            switch (field) {
                case "packages" -> {
                    var res = readStringArray(in, "vifada.packages");
                    if (res.isErr()) return Result.err(res.unwrapErr());
                    packages = res.unwrap();
                }
                case "config" -> config = Optional.of(in.nextString());
                case "priority" -> priority = in.nextInt();
                default -> in.skipValue();
            }
        }
        in.endObject();
        return Result.ok(new VifadaConfig(packages, config, priority));
    }

    // ------------------------------------------------------ utilities

    private static Result<List<String>, ManifestError> readStringArray(JsonReader in, String path) {
        if (in.peek() != JsonToken.BEGIN_ARRAY) {
            return Result.err(new ManifestError.WrongType(path, "array of strings", tokenName(in.peek())));
        }
        in.beginArray();
        List<String> out = new ArrayList<>();
        int i = 0;
        while (in.hasNext()) {
            if (in.peek() != JsonToken.STRING) {
                return Result.err(new ManifestError.WrongType(
                        path + "[" + i + "]", "string", tokenName(in.peek())));
            }
            out.add(in.nextString());
            i++;
        }
        in.endArray();
        return Result.ok(List.copyOf(out));
    }

    /**
     * Читает произвольный JSON-объект как {@code Map<String, Object>}; значения —
     * {@code Boolean}/{@code Long}/{@code Double}/{@code String}/{@code List}/{@code Map}/{@code null}.
     * Используется для поля {@code custom}.
     */
    private static Result<Map<String, Object>, ManifestError> readGenericObject(JsonReader in) {
        if (in.peek() != JsonToken.BEGIN_OBJECT) {
            return Result.err(new ManifestError.WrongType("custom", "object", tokenName(in.peek())));
        }
        Map<String, Object> map = new LinkedHashMap<>();
        in.beginObject();
        while (in.hasNext()) {
            String key = in.nextName();
            map.put(key, readGenericValue(in));
        }
        in.endObject();
        return Result.ok(Map.copyOf(map));
    }

    private static Object readGenericValue(JsonReader in) {
        JsonToken t = in.peek();
        return switch (t) {
            case STRING -> in.nextString();
            case BOOLEAN -> in.nextBoolean();
            case NULL -> { in.nextNull(); yield null; }
            case NUMBER -> parseGenericNumber(in.nextNumberString());
            case BEGIN_ARRAY -> {
                in.beginArray();
                List<Object> list = new ArrayList<>();
                while (in.hasNext()) list.add(readGenericValue(in));
                in.endArray();
                yield List.copyOf(list);
            }
            case BEGIN_OBJECT -> {
                in.beginObject();
                Map<String, Object> nested = new LinkedHashMap<>();
                while (in.hasNext()) {
                    String k = in.nextName();
                    nested.put(k, readGenericValue(in));
                }
                in.endObject();
                yield Map.copyOf(nested);
            }
            default -> throw new JsonException("unexpected " + t + " in generic value", in.line(), in.column());
        };
    }

    private static Object parseGenericNumber(String raw) {
        Objects.requireNonNull(raw, "raw");
        if (raw.indexOf('.') < 0 && raw.indexOf('e') < 0 && raw.indexOf('E') < 0) {
            try {
                return Long.parseLong(raw);
            } catch (NumberFormatException ignore) {
                // fall through to double
            }
        }
        return Double.parseDouble(raw);
    }

    private static String tokenName(JsonToken t) {
        return t == null ? "<null>" : t.name().toLowerCase();
    }
}
