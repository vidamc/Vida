/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.mc;

import static org.assertj.core.api.Assertions.*;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class JsonTreeTest {

    @Test
    @SuppressWarnings("unchecked")
    void parses_primitives() {
        assertThat(JsonTree.parse("\"hello\"")).isEqualTo("hello");
        assertThat(JsonTree.parse("42")).isEqualTo(42L);
        assertThat(JsonTree.parse("1.5")).isEqualTo(1.5);
        assertThat(JsonTree.parse("true")).isEqualTo(Boolean.TRUE);
        assertThat(JsonTree.parse("false")).isEqualTo(Boolean.FALSE);
        assertThat(JsonTree.parse("null")).isNull();

        // Большие целые → BigInteger
        Object big = JsonTree.parse("999999999999999999999");
        assertThat(big).isInstanceOf(BigInteger.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void parses_object_preserves_insertion_order() {
        Map<String, Object> m = (Map<String, Object>) JsonTree.parse(
                "{ \"c\": 1, \"a\": 2, \"b\": 3 }");
        assertThat(m).containsOnlyKeys("c", "a", "b");
        assertThat(List.copyOf(m.keySet())).containsExactly("c", "a", "b");
    }

    @Test
    @SuppressWarnings("unchecked")
    void parses_nested() {
        Map<String, Object> m = (Map<String, Object>) JsonTree.parse(
                "{\"profiles\":{\"p1\":{\"name\":\"A\"}},\"list\":[1,2,3]}");
        assertThat(((Map<String, Object>) m.get("profiles")).get("p1"))
                .isInstanceOf(Map.class);
        assertThat((List<Object>) m.get("list"))
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void round_trips_simple_tree() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", "vida");
        m.put("version", 3L);
        m.put("enabled", true);
        m.put("tags", List.of("alpha", "beta"));

        String json = JsonTree.write(m);
        @SuppressWarnings("unchecked")
        Map<String, Object> back = (Map<String, Object>) JsonTree.parse(json);
        assertThat(back).isEqualTo(m);
    }

    @Test
    void writes_with_two_space_indent() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("a", 1L);
        String out = JsonTree.write(m);
        assertThat(out).isEqualTo("{\n  \"a\": 1\n}\n");
    }

    @Test
    void writes_empty_collections_inline() {
        assertThat(JsonTree.write(new LinkedHashMap<>())).isEqualTo("{}\n");
        assertThat(JsonTree.write(List.of())).isEqualTo("[]\n");
    }

    @Test
    void escapes_special_chars_in_strings() {
        String out = JsonTree.write(Map.of("k", "he said \"hi\"\nand\tleft"));
        @SuppressWarnings("unchecked")
        Map<String, Object> back = (Map<String, Object>) JsonTree.parse(out);
        assertThat(back.get("k")).isEqualTo("he said \"hi\"\nand\tleft");
    }

    @Test
    void doubles_render_without_losing_precision() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("pi", 3.14159265);
        m.put("whole", 2.0);
        String out = JsonTree.write(m);
        assertThat(out).contains("3.14159265").contains("2.0");
    }

    @Test
    @SuppressWarnings("unchecked")
    void get_or_create_object_creates_missing_paths() {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> profiles = JsonTree.getOrCreateObject(root, "profiles");
        profiles.put("p1", "x");

        assertThat(root).containsKey("profiles");
        assertThat(((Map<String, Object>) root.get("profiles"))).containsEntry("p1", "x");

        // Повторный вызов — не пересоздаёт существующий объект.
        Map<String, Object> again = JsonTree.getOrCreateObject(root, "profiles");
        assertThat(again).isSameAs(profiles);
    }

    @Test
    void get_or_create_object_throws_on_non_object_in_path() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("a", "string-here");
        assertThatThrownBy(() -> JsonTree.getOrCreateObject(root, "a", "b"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejects_nan_and_infinity_on_write() {
        assertThatThrownBy(() -> JsonTree.write(Map.of("k", Double.NaN)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> JsonTree.write(Map.of("k", Double.POSITIVE_INFINITY)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
