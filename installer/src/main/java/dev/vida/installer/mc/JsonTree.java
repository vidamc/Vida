/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.mc;

import dev.vida.manifest.json.JsonReader;
import dev.vida.manifest.json.JsonToken;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Минимальная tree-model для JSON: парсит в {@code Map&lt;String,Object&gt;} /
 * {@code List&lt;Object&gt;}, сериализует обратно с отступами в 2 пробела.
 *
 * <p>Используется только для {@code launcher_profiles.json}, где нам важно
 * сохранить <em>все</em> существующие поля/профили и только добавить/
 * обновить одну запись. Для hot path (парсинг vida.mod.json на каждом запуске)
 * применяется стриминговый {@link JsonReader} из {@code :manifest}.
 *
 * <p>Поддерживаемые Java-типы в узлах:
 * <ul>
 *   <li>{@link Map}{@code <String,Object>} — JSON object (порядок ключей сохраняется);</li>
 *   <li>{@link List}{@code <Object>} — JSON array;</li>
 *   <li>{@link String} — JSON string;</li>
 *   <li>{@link Long}, {@link Double}, {@link BigInteger} — JSON number;</li>
 *   <li>{@link Boolean} — {@code true}/{@code false};</li>
 *   <li>{@code null} — {@code null}.</li>
 * </ul>
 */
public final class JsonTree {

    private JsonTree() {}

    // ============================================================ read

    public static Object parse(String json) {
        return parse(new StringReader(json));
    }

    public static Object parse(Reader reader) {
        try (JsonReader r = JsonReader.of(reader)) {
            return readValue(r);
        }
    }

    private static Object readValue(JsonReader r) {
        JsonToken t = r.peek();
        return switch (t) {
            case BEGIN_OBJECT -> readObject(r);
            case BEGIN_ARRAY  -> readArray(r);
            case STRING       -> r.nextString();
            case NUMBER       -> readNumber(r.nextNumberString());
            case BOOLEAN      -> r.nextBoolean();
            case NULL         -> { r.nextNull(); yield null; }
            default           -> throw new IllegalStateException("unexpected token " + t);
        };
    }

    private static Map<String, Object> readObject(JsonReader r) {
        Map<String, Object> m = new LinkedHashMap<>();
        r.beginObject();
        while (r.hasNext()) {
            String key = r.nextName();
            m.put(key, readValue(r));
        }
        r.endObject();
        return m;
    }

    private static List<Object> readArray(JsonReader r) {
        List<Object> list = new ArrayList<>();
        r.beginArray();
        while (r.hasNext()) {
            list.add(readValue(r));
        }
        r.endArray();
        return list;
    }

    private static Object readNumber(String raw) {
        // Целые → Long, если влезают; иначе BigInteger. Иначе Double.
        boolean floating = raw.indexOf('.') >= 0 || raw.indexOf('e') >= 0 || raw.indexOf('E') >= 0;
        if (!floating) {
            try {
                return Long.parseLong(raw);
            } catch (NumberFormatException e) {
                return new BigInteger(raw);
            }
        }
        return Double.parseDouble(raw);
    }

    // ============================================================ write

    /** Сериализация с отступом 2 пробела и {@code \n} в конце файла. */
    public static String write(Object value) {
        StringBuilder sb = new StringBuilder(256);
        writeValue(sb, value, 0);
        sb.append('\n');
        return sb.toString();
    }

    private static void writeValue(StringBuilder sb, Object v, int indent) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            writeString(sb, s);
        } else if (v instanceof Boolean b) {
            sb.append(b.booleanValue() ? "true" : "false");
        } else if (v instanceof Long l) {
            sb.append(l.longValue());
        } else if (v instanceof Integer i) {
            sb.append(i.intValue());
        } else if (v instanceof BigInteger bi) {
            sb.append(bi.toString());
        } else if (v instanceof Double d) {
            writeDouble(sb, d);
        } else if (v instanceof Float f) {
            writeDouble(sb, (double) f);
        } else if (v instanceof Number n) {
            sb.append(n.toString());
        } else if (v instanceof Map<?, ?> m) {
            writeObject(sb, m, indent);
        } else if (v instanceof List<?> l) {
            writeArray(sb, l, indent);
        } else {
            throw new IllegalArgumentException("unsupported JSON node: " + v.getClass());
        }
    }

    private static void writeDouble(StringBuilder sb, double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw new IllegalArgumentException("NaN/Infinity not valid in JSON");
        }
        // Печатаем integral-выглядящие double'ы как 1.0 (а не "1") — JSON-строго.
        if (d == Math.floor(d) && !Double.isInfinite(d)
                && Math.abs(d) < 1e16) {
            sb.append(((long) d)).append(".0");
        } else {
            sb.append(Double.toString(d));
        }
    }

    private static void writeObject(StringBuilder sb, Map<?, ?> m, int indent) {
        if (m.isEmpty()) { sb.append("{}"); return; }
        sb.append("{\n");
        int inner = indent + 1;
        int i = 0, n = m.size();
        for (Map.Entry<?, ?> e : m.entrySet()) {
            indent(sb, inner);
            writeString(sb, String.valueOf(e.getKey()));
            sb.append(": ");
            writeValue(sb, e.getValue(), inner);
            if (++i < n) sb.append(',');
            sb.append('\n');
        }
        indent(sb, indent);
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, List<?> list, int indent) {
        if (list.isEmpty()) { sb.append("[]"); return; }
        sb.append("[\n");
        int inner = indent + 1;
        int i = 0, n = list.size();
        for (Object v : list) {
            indent(sb, inner);
            writeValue(sb, v, inner);
            if (++i < n) sb.append(',');
            sb.append('\n');
        }
        indent(sb, indent);
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    private static void indent(StringBuilder sb, int level) {
        for (int i = 0; i < level; i++) sb.append("  ");
    }

    // ============================================================ ops

    /**
     * Удобный навигатор: возвращает вложенный объект по пути из ключей,
     * создавая отсутствующие объекты по дороге.
     *
     * @throws IllegalStateException если на пути встретилось не-object.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getOrCreateObject(Map<String, Object> root, String... path) {
        Map<String, Object> cur = root;
        for (String key : path) {
            Object v = cur.get(key);
            if (v == null) {
                Map<String, Object> child = new LinkedHashMap<>();
                cur.put(key, child);
                cur = child;
            } else if (v instanceof Map<?, ?> m) {
                cur = (Map<String, Object>) m;
            } else {
                throw new IllegalStateException(
                        "path '" + String.join(".", path) + "' contains non-object at key '" + key + "'");
            }
        }
        return cur;
    }

    /** Утилита: преобразует {@code Map<String,Object>} в человеко-читаемый JSON. */
    public static String pretty(Map<String, Object> root) throws IOException {
        return write(root);
    }
}
