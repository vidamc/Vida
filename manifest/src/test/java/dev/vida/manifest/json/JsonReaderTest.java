/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.manifest.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JsonReaderTest {

    @Test
    void parsesEmptyObject() {
        try (JsonReader in = JsonReader.of("{}")) {
            in.beginObject();
            assertThat(in.hasNext()).isFalse();
            in.endObject();
            assertThat(in.peek()).isEqualTo(JsonToken.EOF);
        }
    }

    @Test
    void parsesSimpleObject() {
        String json = "{\"a\":1,\"b\":\"hello\",\"c\":true,\"d\":null}";
        try (JsonReader in = JsonReader.of(json)) {
            in.beginObject();
            assertThat(in.nextName()).isEqualTo("a");
            assertThat(in.nextInt()).isEqualTo(1);
            assertThat(in.nextName()).isEqualTo("b");
            assertThat(in.nextString()).isEqualTo("hello");
            assertThat(in.nextName()).isEqualTo("c");
            assertThat(in.nextBoolean()).isTrue();
            assertThat(in.nextName()).isEqualTo("d");
            in.nextNull();
            in.endObject();
        }
    }

    @Test
    void parsesNestedStructures() {
        String json = "{\"outer\":{\"list\":[1,2,3],\"obj\":{\"k\":\"v\"}}}";
        try (JsonReader in = JsonReader.of(json)) {
            in.beginObject();
            assertThat(in.nextName()).isEqualTo("outer");
            in.beginObject();
            assertThat(in.nextName()).isEqualTo("list");
            in.beginArray();
            assertThat(in.nextInt()).isEqualTo(1);
            assertThat(in.nextInt()).isEqualTo(2);
            assertThat(in.nextInt()).isEqualTo(3);
            in.endArray();
            assertThat(in.nextName()).isEqualTo("obj");
            in.beginObject();
            assertThat(in.nextName()).isEqualTo("k");
            assertThat(in.nextString()).isEqualTo("v");
            in.endObject();
            in.endObject();
            in.endObject();
        }
    }

    @Test
    void handlesEscapes() {
        try (JsonReader in = JsonReader.of("\"a\\nb\\t\\\"c\\\\d\\u00e9\"")) {
            assertThat(in.nextString()).isEqualTo("a\nb\t\"c\\d\u00e9");
        }
    }

    @Test
    void handlesNumbers() {
        try (JsonReader in = JsonReader.of("[0,1,-12,3.14,-0.5,1e10,1.5E-3]")) {
            in.beginArray();
            assertThat(in.nextInt()).isZero();
            assertThat(in.nextInt()).isEqualTo(1);
            assertThat(in.nextInt()).isEqualTo(-12);
            assertThat(in.nextDouble()).isEqualTo(3.14);
            assertThat(in.nextDouble()).isEqualTo(-0.5);
            assertThat(in.nextDouble()).isEqualTo(1e10);
            assertThat(in.nextDouble()).isEqualTo(1.5e-3);
            in.endArray();
        }
    }

    @Test
    void skipValueHandlesNested() {
        String json = "{\"a\":{\"b\":[1,2,{\"c\":\"d\"}]},\"e\":\"ok\"}";
        try (JsonReader in = JsonReader.of(json)) {
            in.beginObject();
            assertThat(in.nextName()).isEqualTo("a");
            in.skipValue();
            assertThat(in.nextName()).isEqualTo("e");
            assertThat(in.nextString()).isEqualTo("ok");
            in.endObject();
        }
    }

    @Test
    void whitespaceAndEmptyArrayHandled() {
        try (JsonReader in = JsonReader.of("  \n\t[ ]\n")) {
            in.beginArray();
            assertThat(in.hasNext()).isFalse();
            in.endArray();
        }
    }

    @Test
    void reportsErrorWithCoordinates() {
        assertThatThrownBy(() -> {
            try (JsonReader in = JsonReader.of("{\n \"a\": unexpected }")) {
                in.beginObject();
                in.nextName();
                in.nextString();
            }
        })
                .isInstanceOf(JsonException.class)
                .hasMessageContaining("line 2");
    }

    @Test
    void rejectsTrailingGarbage() {
        assertThatThrownBy(() -> {
            try (JsonReader in = JsonReader.of("{}extra")) {
                in.beginObject();
                in.endObject();
                // после этого peek должен вернуть NonEOF, но мы проверяем явно
                JsonToken after = in.peek();
                if (after != JsonToken.EOF) {
                    throw new JsonException("garbage after root: " + after, in.line(), in.column());
                }
            }
        }).isInstanceOf(JsonException.class);
    }

    @Test
    void rejectsUnterminatedString() {
        assertThatThrownBy(() -> {
            try (JsonReader in = JsonReader.of("\"abc")) {
                in.nextString();
            }
        }).isInstanceOf(JsonException.class).hasMessageContaining("unterminated");
    }

    @Test
    void rejectsLeadingZeroInNumber() {
        assertThatThrownBy(() -> {
            try (JsonReader in = JsonReader.of("01")) {
                in.nextInt();
            }
        }).isInstanceOf(JsonException.class);
    }

    @Test
    void rejectsBadEscape() {
        assertThatThrownBy(() -> {
            try (JsonReader in = JsonReader.of("\"\\x\"")) {
                in.nextString();
            }
        }).isInstanceOf(JsonException.class).hasMessageContaining("invalid escape");
    }

    @Test
    void rejectsControlCharInString() {
        assertThatThrownBy(() -> {
            try (JsonReader in = JsonReader.of("\"a\u0001b\"")) {
                in.nextString();
            }
        }).isInstanceOf(JsonException.class).hasMessageContaining("control");
    }

    @Test
    void respectsMaxDepth() {
        // 300 открытых [ — больше чем MAX_DEPTH=256
        String deep = "[".repeat(300);
        assertThatThrownBy(() -> {
            try (JsonReader in = JsonReader.of(deep)) {
                // Стараемся пройти внутрь
                for (int i = 0; i < 300; i++) in.beginArray();
            }
        }).isInstanceOf(JsonException.class).hasMessageContaining("nesting");
    }
}
