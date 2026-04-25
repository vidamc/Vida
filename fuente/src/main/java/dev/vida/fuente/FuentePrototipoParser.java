/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.fuente;

import dev.vida.bloque.MaterialBloque;
import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import dev.vida.core.Result;
import dev.vida.discovery.ZipReader;
import dev.vida.objeto.TipoObjeto;
import dev.vida.manifest.ModManifest;
import dev.vida.manifest.json.JsonException;
import dev.vida.manifest.json.JsonReader;
import dev.vida.manifest.json.JsonToken;
import dev.vida.manifest.json.VidaJson;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Парсер data-driven контента из datapack JSON: блоки, предметы, shaped-рецепты,
 * таблицы лута (извлечение item-id из стандартных {@code pools/entries}),
 * worldgen JSON (эвристики ссылок на loot и блоки, см. {@link FuenteWorldgenEscan}).
 */
@ApiStatus.Stable
public final class FuentePrototipoParser {

    private static final String CONFIG_KEY = "vida:dataDriven";

    private FuentePrototipoParser() {}

    public static Result<FuenteContenidoMod, FuenteError> leer(ModManifest manifest, ZipReader zip) {
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(zip, "zip");

        Result<Config, FuenteError> cfg = parseConfig(manifest);
        if (cfg.isErr()) {
            return Result.err(cfg.unwrapErr());
        }
        Config config = cfg.unwrap();
        if (!config.habilitado()) {
            return Result.ok(FuenteContenidoMod.DESHABILITADO);
        }

        String root = config.rootDatapack();
        String prefBloques = root + "/bloques/";
        String prefObjetos = root + "/objetos/";
        String prefRecetas = root + "/recipes/";
        String prefLoot = root + "/loot_tables/";
        String prefWorldgen = root + "/worldgen/";

        List<FuenteBloque> bloques = new ArrayList<>();
        List<FuenteObjeto> objetos = new ArrayList<>();
        List<FuenteRecetaShaped> recetas = new ArrayList<>();
        List<FuenteLootTable> loot = new ArrayList<>();
        List<FuenteWorldgenHuella> worldgen = new ArrayList<>();

        for (String entry : zip.entries()) {
            if (!entry.endsWith(".json")) {
                continue;
            }
            if (entry.startsWith(prefBloques)) {
                Result<FuenteBloque, FuenteError> r = parseBloque(manifest.id(), entry, zip);
                if (r.isErr()) return Result.err(r.unwrapErr());
                bloques.add(r.unwrap());
            } else if (entry.startsWith(prefObjetos)) {
                Result<FuenteObjeto, FuenteError> r = parseObjeto(manifest.id(), entry, zip);
                if (r.isErr()) return Result.err(r.unwrapErr());
                objetos.add(r.unwrap());
            } else if (entry.startsWith(prefRecetas)) {
                Result<FuenteRecetaShaped, FuenteError> r = parseRecetaShaped(manifest.id(), root, entry, zip);
                if (r.isErr()) return Result.err(r.unwrapErr());
                recetas.add(r.unwrap());
            } else if (entry.startsWith(prefLoot)) {
                Result<FuenteLootTable, FuenteError> r =
                        parseLootTable(manifest.id(), root, entry, zip);
                if (r.isErr()) return Result.err(r.unwrapErr());
                loot.add(r.unwrap());
            } else if (entry.startsWith(prefWorldgen)) {
                Result<FuenteWorldgenHuella, FuenteError> r =
                        parseWorldgenHuella(manifest.id(), entry, zip);
                if (r.isErr()) return Result.err(r.unwrapErr());
                worldgen.add(r.unwrap());
            }
        }

        bloques.sort(Comparator.comparing(b -> b.id().toString()));
        objetos.sort(Comparator.comparing(o -> o.id().toString()));
        recetas.sort(Comparator.comparing(r -> r.id().toString()));
        loot.sort(Comparator.comparing(l -> l.id().toString()));
        worldgen.sort(Comparator.comparing(FuenteWorldgenHuella::pathZip));
        FuenteContenidoMod contenido =
                new FuenteContenidoMod(true, root, bloques, objetos, recetas, loot, worldgen);
        Result<Void, FuenteError> validacionLoot = FuenteLootPipeline.validarReferenciasInternas(contenido);
        if (validacionLoot.isErr()) {
            return Result.err(validacionLoot.unwrapErr());
        }
        Result<Void, FuenteError> validacionWg =
                FuenteWorldgenPipeline.validarVsDatapack(contenido, manifest.id());
        if (validacionWg.isErr()) {
            return Result.err(validacionWg.unwrapErr());
        }
        return Result.ok(contenido);
    }

    private static Result<FuenteWorldgenHuella, FuenteError> parseWorldgenHuella(
            String defaultNamespace, String path, ZipReader zip) {
        Result<Map<String, Object>, FuenteError> root = parseObject(path, zip);
        if (root.isErr()) {
            return Result.err(root.unwrapErr());
        }
        LinkedHashSet<Identifier> loot = new LinkedHashSet<>();
        LinkedHashSet<Identifier> bloques = new LinkedHashSet<>();
        FuenteWorldgenEscan.escanear(root.unwrap(), defaultNamespace, loot, bloques);
        return Result.ok(FuenteWorldgenHuella.dedup(path, loot, bloques));
    }

    private static Result<Config, FuenteError> parseConfig(ModManifest manifest) {
        Object raw = manifest.custom().get(CONFIG_KEY);
        if (raw == null) {
            return Result.ok(new Config(false, ""));
        }
        if (!(raw instanceof Map<?, ?> m)) {
            return Result.err(new FuenteError.ConfigInvalida(CONFIG_KEY + " must be object"));
        }

        try {
            boolean enabled = getBoolean(m, "enabled", false);
            if (!enabled) {
                return Result.ok(new Config(false, ""));
            }
            String root = getString(m, "datapackRoot", "data/" + manifest.id() + "/vida");
            if (root.isBlank()) {
                return Result.err(new FuenteError.ConfigInvalida("datapackRoot is blank"));
            }
            return Result.ok(new Config(true, root));
        } catch (IllegalArgumentException ex) {
            return Result.err(new FuenteError.ConfigInvalida(ex.getMessage()));
        }
    }

    private static Result<FuenteBloque, FuenteError> parseBloque(
            String defaultNamespace,
            String path,
            ZipReader zip) {
        Result<Map<String, Object>, FuenteError> doc = parseObject(path, zip);
        if (doc.isErr()) return Result.err(doc.unwrapErr());
        Map<String, Object> obj = doc.unwrap();

        String idRaw = getStringRequired(obj, path, "id");
        String material = getString(obj, "material", "GENERICO");
        try {
            MaterialBloque.valueOf(material);
        } catch (IllegalArgumentException ex) {
            return Result.err(new FuenteError.TipoDesconocido(path + ".material", material));
        }
        float dureza = getFloat(obj, "dureza", 1.0f);
        try {
            return Result.ok(new FuenteBloque(
                    Identifier.parseWithDefault(idRaw, defaultNamespace),
                    material,
                    dureza));
        } catch (IllegalArgumentException ex) {
            return Result.err(new FuenteError.JsonInvalido(path, ex.getMessage()));
        }
    }

    private static Result<FuenteObjeto, FuenteError> parseObjeto(
            String defaultNamespace,
            String path,
            ZipReader zip) {
        Result<Map<String, Object>, FuenteError> doc = parseObject(path, zip);
        if (doc.isErr()) return Result.err(doc.unwrapErr());
        Map<String, Object> obj = doc.unwrap();

        String idRaw = getStringRequired(obj, path, "id");
        String tipo = getString(obj, "tipo", "GENERICO");
        try {
            TipoObjeto.valueOf(tipo);
        } catch (IllegalArgumentException ex) {
            return Result.err(new FuenteError.TipoDesconocido(path + ".tipo", tipo));
        }
        int maxPila = getInt(obj, "maxPila", 64);
        try {
            return Result.ok(new FuenteObjeto(
                    Identifier.parseWithDefault(idRaw, defaultNamespace),
                    tipo,
                    maxPila));
        } catch (IllegalArgumentException ex) {
            return Result.err(new FuenteError.JsonInvalido(path, ex.getMessage()));
        }
    }

    private static Result<FuenteRecetaShaped, FuenteError> parseRecetaShaped(
            String defaultNamespace,
            String root,
            String path,
            ZipReader zip) {
        Result<Map<String, Object>, FuenteError> doc = parseObject(path, zip);
        if (doc.isErr()) return Result.err(doc.unwrapErr());
        Map<String, Object> obj = doc.unwrap();

        String type = getString(obj, "type", "vida:shaped");
        if (!"vida:shaped".equals(type)) {
            return Result.err(new FuenteError.JsonInvalido(path, "unsupported recipe type: " + type));
        }
        try {
            String idRaw = getString(obj, "id", derivarIdReceta(defaultNamespace, root, path));
            List<String> pattern = readStringListRequired(obj, path, "pattern");
            Map<Character, Identifier> keys = readKeys(defaultNamespace, obj, path);
            FuenteRecetaShaped.Resultado result = readResultado(defaultNamespace, obj, path);
            return Result.ok(new FuenteRecetaShaped(
                    Identifier.parseWithDefault(idRaw, defaultNamespace),
                    pattern,
                    keys,
                    result));
        } catch (IllegalArgumentException ex) {
            return Result.err(new FuenteError.JsonInvalido(path, ex.getMessage()));
        }
    }

    private static Map<Character, Identifier> readKeys(
            String defaultNamespace,
            Map<String, Object> obj,
            String path) {
        Object raw = obj.get("key");
        if (!(raw instanceof Map<?, ?> keyMap)) {
            throw new IllegalArgumentException(path + ": key must be object");
        }
        Map<Character, Identifier> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : keyMap.entrySet()) {
            String k = String.valueOf(e.getKey());
            if (k.length() != 1) {
                throw new IllegalArgumentException(path + ": key '" + k + "' must be single char");
            }
            if (!(e.getValue() instanceof String itemId)) {
                throw new IllegalArgumentException(path + ": key." + k + " must be string");
            }
            out.put(k.charAt(0), Identifier.parseWithDefault(itemId, defaultNamespace));
        }
        return out;
    }

    private static FuenteRecetaShaped.Resultado readResultado(
            String defaultNamespace,
            Map<String, Object> obj,
            String path) {
        Object raw = obj.get("result");
        if (!(raw instanceof Map<?, ?> resultMap)) {
            throw new IllegalArgumentException(path + ": result must be object");
        }
        String idRaw = asStringRequired(resultMap, path + ".result", "id");
        int cantidad = asInt(resultMap.get("count"), 1, path + ".result.count");
        return new FuenteRecetaShaped.Resultado(
                Identifier.parseWithDefault(idRaw, defaultNamespace),
                cantidad);
    }

    private static String derivarIdReceta(String defaultNamespace, String root, String path) {
        String prefix = root + "/recipes/";
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            return defaultNamespace + ":invalid";
        }
        String local = path.substring(prefix.length(), path.length() - ".json".length());
        return defaultNamespace + ":" + local;
    }

    private static Result<FuenteLootTable, FuenteError> parseLootTable(
            String defaultNamespace,
            String root,
            String path,
            ZipReader zip) {
        Result<Map<String, Object>, FuenteError> doc = parseObject(path, zip);
        if (doc.isErr()) {
            return Result.err(doc.unwrapErr());
        }
        Map<String, Object> obj = doc.unwrap();
        try {
            List<Identifier> items = extractLootItems(defaultNamespace, obj);
            List<Identifier> tablas = extractReferenciasTablasLoot(defaultNamespace, obj);
            String idRaw = derivarIdLoot(defaultNamespace, root, path);
            return Result.ok(new FuenteLootTable(
                    Identifier.parseWithDefault(idRaw, defaultNamespace),
                    items,
                    tablas));
        } catch (IllegalArgumentException ex) {
            return Result.err(new FuenteError.JsonInvalido(path, ex.getMessage()));
        }
    }

    private static String derivarIdLoot(String defaultNamespace, String root, String path) {
        String prefix = root + "/loot_tables/";
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            return defaultNamespace + ":invalid_loot";
        }
        String local = path.substring(prefix.length(), path.length() - ".json".length());
        return defaultNamespace + ":" + local;
    }

    private static List<Identifier> extractLootItems(String ns, Map<String, Object> rootObj) {
        List<Identifier> out = new ArrayList<>();
        Object pools = rootObj.get("pools");
        if (!(pools instanceof List<?> pl)) {
            return out;
        }
        for (Object poolObj : pl) {
            if (!(poolObj instanceof Map<?, ?> pool)) {
                continue;
            }
            collectLootEntries(ns, pool.get("entries"), out);
        }
        return out;
    }

    private static List<Identifier> extractReferenciasTablasLoot(String ns, Map<String, Object> rootObj) {
        List<Identifier> out = new ArrayList<>();
        Object pools = rootObj.get("pools");
        if (!(pools instanceof List<?> pl)) {
            return out;
        }
        for (Object poolObj : pl) {
            if (!(poolObj instanceof Map<?, ?> pool)) {
                continue;
            }
            collectLootTableRefs(ns, pool.get("entries"), out);
        }
        return out;
    }

    private static void collectLootTableRefs(String ns, Object entries, List<Identifier> out) {
        if (!(entries instanceof List<?> el)) {
            return;
        }
        for (Object e : el) {
            if (!(e instanceof Map<?, ?> em)) {
                continue;
            }
            Object type = em.get("type");
            String ts = String.valueOf(type);
            if ("minecraft:loot_table".equals(ts)) {
                Object name = primerNombreLootTable(em);
                if (name instanceof String s) {
                    out.add(Identifier.parseWithDefault(s, ns));
                }
            }
            collectLootTableRefs(ns, em.get("children"), out);
        }
    }

    /** Поддержка {@code value}, {@code name}, вложенного объекта {@code value}. */
    private static Object primerNombreLootTable(Map<?, ?> entry) {
        Object v = entry.get("value");
        if (v instanceof String s) {
            return s;
        }
        if (v instanceof Map<?, ?> m) {
            Object n = m.get("name");
            if (n instanceof String s) {
                return s;
            }
        }
        Object n = entry.get("name");
        if (n instanceof String s) {
            return s;
        }
        return null;
    }

    private static void collectLootEntries(String ns, Object entries, List<Identifier> out) {
        if (!(entries instanceof List<?> el)) {
            return;
        }
        for (Object e : el) {
            if (!(e instanceof Map<?, ?> em)) {
                continue;
            }
            Object type = em.get("type");
            if ("minecraft:item".equals(String.valueOf(type))) {
                Object name = em.get("name");
                if (name instanceof String s) {
                    out.add(Identifier.parseWithDefault(s, ns));
                }
            }
            collectLootEntries(ns, em.get("children"), out);
        }
    }

    private static Result<Map<String, Object>, FuenteError> parseObject(String path, ZipReader zip) {
        final byte[] bytes;
        try {
            bytes = zip.read(path);
        } catch (Exception ex) {
            return Result.err(new FuenteError.JsonInvalido(path, ex.getMessage()));
        }
        String json = new String(bytes, StandardCharsets.UTF_8);
        try (JsonReader in = VidaJson.reader(json)) {
            if (in.peek() != JsonToken.BEGIN_OBJECT) {
                return Result.err(new FuenteError.JsonInvalido(path, "root must be object"));
            }
            return Result.ok(readObject(in));
        } catch (JsonException | IllegalArgumentException ex) {
            return Result.err(new FuenteError.JsonInvalido(path, ex.getMessage()));
        }
    }

    private static Map<String, Object> readObject(JsonReader in) {
        in.beginObject();
        Map<String, Object> out = new LinkedHashMap<>();
        while (in.hasNext()) {
            String key = in.nextName();
            out.put(key, readValue(in));
        }
        in.endObject();
        return out;
    }

    private static Object readValue(JsonReader in) {
        return switch (in.peek()) {
            case STRING -> in.nextString();
            case NUMBER -> {
                String raw = in.nextNumberString();
                if (raw.contains(".") || raw.contains("e") || raw.contains("E")) {
                    yield Double.parseDouble(raw);
                }
                yield Long.parseLong(raw);
            }
            case BOOLEAN -> in.nextBoolean();
            case NULL -> {
                in.nextNull();
                yield null;
            }
            case BEGIN_ARRAY -> {
                List<Object> list = new ArrayList<>();
                in.beginArray();
                while (in.hasNext()) {
                    list.add(readValue(in));
                }
                in.endArray();
                yield List.copyOf(list);
            }
            case BEGIN_OBJECT -> Map.copyOf(readObject(in));
            default -> throw new IllegalArgumentException("unexpected token: " + in.peek());
        };
    }

    private static String getStringRequired(Map<String, Object> map, String path, String key) {
        Object value = map.get(key);
        if (!(value instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException(path + ": missing or invalid '" + key + "'");
        }
        return s;
    }

    private static List<String> readStringListRequired(Map<String, Object> map, String path, String key) {
        Object value = map.get(key);
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException(path + ": missing or invalid '" + key + "'");
        }
        List<String> out = new ArrayList<>(list.size());
        for (Object item : list) {
            if (!(item instanceof String s)) {
                throw new IllegalArgumentException(path + "." + key + " must contain only strings");
            }
            out.add(s);
        }
        return out;
    }

    private static String getString(Map<?, ?> map, String key, String def) {
        Object value = map.get(key);
        if (value == null) return def;
        if (!(value instanceof String s)) {
            throw new IllegalArgumentException(key + " must be string");
        }
        return s;
    }

    private static int getInt(Map<String, Object> map, String key, int def) {
        return asInt(map.get(key), def, key);
    }

    private static int asInt(Object value, int def, String path) {
        if (value == null) return def;
        if (value instanceof Long l) return Math.toIntExact(l);
        if (value instanceof Double d) return (int) Math.round(d);
        throw new IllegalArgumentException(path + " must be number");
    }

    private static float getFloat(Map<String, Object> map, String key, float def) {
        Object value = map.get(key);
        if (value == null) return def;
        if (value instanceof Long l) return l.floatValue();
        if (value instanceof Double d) return d.floatValue();
        throw new IllegalArgumentException(key + " must be number");
    }

    private static boolean getBoolean(Map<?, ?> map, String key, boolean def) {
        Object value = map.get(key);
        if (value == null) return def;
        if (!(value instanceof Boolean b)) {
            throw new IllegalArgumentException(key + " must be boolean");
        }
        return b;
    }

    private static String asStringRequired(Map<?, ?> map, String path, String key) {
        Object value = map.get(key);
        if (!(value instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException(path + ": missing or invalid '" + key + "'");
        }
        return s;
    }

    private record Config(boolean habilitado, String rootDatapack) {}
}
