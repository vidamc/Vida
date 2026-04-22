/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.manifest;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.core.Result;
import dev.vida.core.Version;
import org.junit.jupiter.api.Test;

class ManifestParserTest {

    @Test
    void parsesMinimalManifest() {
        String json = """
                {
                  "schema": 1,
                  "id": "ejemplo",
                  "version": "1.0.0",
                  "name": "Ejemplo"
                }
                """;
        Result<ModManifest, ManifestError> r = ManifestParser.parse(json);
        assertThat(r.isOk()).as("parse error: %s", r.err()).isTrue();

        ModManifest m = r.unwrap();
        assertThat(m.id()).isEqualTo("ejemplo");
        assertThat(m.version()).isEqualTo(Version.of(1, 0, 0));
        assertThat(m.name()).isEqualTo("Ejemplo");
        assertThat(m.description()).isEmpty();
        assertThat(m.authors()).isEmpty();
        assertThat(m.dependencies().isEmpty()).isTrue();
        assertThat(m.entrypoints().isEmpty()).isTrue();
    }

    @Test
    void parsesFullManifest() {
        String json = """
                {
                  "schema": 1,
                  "id": "ejemplo.sagrada",
                  "version": "0.3.2-beta.1+mc1.21.1",
                  "name": "Espada Sagrada",
                  "description": "Demo mod",
                  "license": "Apache-2.0",
                  "authors": [
                    "Alicia",
                    { "name": "Bob", "contact": "bob@example.com" }
                  ],
                  "entrypoints": {
                    "preLaunch": ["ejemplo.PreLaunch"],
                    "main": ["ejemplo.Main"],
                    "client": ["ejemplo.client.ClientMain"]
                  },
                  "dependencies": {
                    "required": {
                      "minecraft": "=1.21.1",
                      "vida-loader": "^0.1.0"
                    },
                    "optional": {
                      "otro-mod": "~1.2"
                    },
                    "incompatibilities": {
                      "malo-mod": "*"
                    }
                  },
                  "vifada": {
                    "packages": ["ejemplo.vifada"],
                    "config": "ejemplo.vifada.json",
                    "priority": 500
                  },
                  "puertas": ["ejemplo.puertas.cfg"],
                  "modules": ["ejemplo-extra"],
                  "custom": {
                    "foo": 42,
                    "bar": { "nested": true },
                    "arr": [1, 2, "three"]
                  }
                }
                """;
        Result<ModManifest, ManifestError> r = ManifestParser.parse(json);
        assertThat(r.isOk()).as("err=%s", r.err()).isTrue();

        ModManifest m = r.unwrap();
        assertThat(m.id()).isEqualTo("ejemplo.sagrada");
        assertThat(m.version().major()).isEqualTo(0);
        assertThat(m.version().preRelease()).containsExactly("beta", "1");
        assertThat(m.version().buildMeta()).containsExactly("mc1", "21", "1");

        assertThat(m.authors()).hasSize(2);
        assertThat(m.authors().get(0).name()).isEqualTo("Alicia");
        assertThat(m.authors().get(0).contact()).isEmpty();
        assertThat(m.authors().get(1).contact()).contains("bob@example.com");

        assertThat(m.entrypoints().main()).containsExactly("ejemplo.Main");
        assertThat(m.entrypoints().client()).containsExactly("ejemplo.client.ClientMain");
        assertThat(m.entrypoints().server()).isEmpty();

        assertThat(m.dependencies().required()).containsOnlyKeys("minecraft", "vida-loader");
        assertThat(m.dependencies().optional()).containsKey("otro-mod");
        assertThat(m.dependencies().incompatibilities()).containsKey("malo-mod");

        assertThat(m.vifada().packages()).containsExactly("ejemplo.vifada");
        assertThat(m.vifada().config()).contains("ejemplo.vifada.json");
        assertThat(m.vifada().priority()).isEqualTo(500);

        assertThat(m.puertas()).containsExactly("ejemplo.puertas.cfg");
        assertThat(m.modules()).containsExactly("ejemplo-extra");

        assertThat(m.custom()).containsKey("foo").containsKey("bar").containsKey("arr");
        assertThat(m.custom().get("foo")).isEqualTo(42L);
    }

    @Test
    void reportsMissingRequiredFields() {
        String json = "{\"schema\":1,\"name\":\"x\"}";
        Result<ModManifest, ManifestError> r = ManifestParser.parse(json);
        assertThat(r.isErr()).isTrue();
        assertThat(r.unwrapErr()).isInstanceOf(ManifestError.MissingField.class);
        assertThat(r.unwrapErr().message()).contains("id");
    }

    @Test
    void rejectsInvalidVersion() {
        String json = """
                { "schema":1, "id":"x", "version":"not-a-version", "name":"X" }
                """;
        Result<ModManifest, ManifestError> r = ManifestParser.parse(json);
        assertThat(r.isErr()).isTrue();
        assertThat(r.unwrapErr()).isInstanceOf(ManifestError.InvalidValue.class);
    }

    @Test
    void rejectsInvalidVersionRange() {
        String json = """
                {
                  "schema":1, "id":"x", "version":"1.0.0", "name":"X",
                  "dependencies": { "required": { "other": "^^^bad" } }
                }
                """;
        Result<ModManifest, ManifestError> r = ManifestParser.parse(json);
        assertThat(r.isErr()).isTrue();
        assertThat(r.unwrapErr()).isInstanceOf(ManifestError.InvalidValue.class);
        assertThat(r.unwrapErr().message()).contains("dependencies.required.other");
    }

    @Test
    void rejectsUnknownSchemaAsUnsupported() {
        String json = """
                { "schema": 99, "id":"x", "version":"1.0.0", "name":"X" }
                """;
        Result<ModManifest, ManifestError> r = ManifestParser.parse(json);
        assertThat(r.isErr()).isTrue();
        assertThat(r.unwrapErr()).isInstanceOf(ManifestError.UnsupportedSchema.class);
    }

    @Test
    void reportsSyntaxErrorWithLineCol() {
        String json = "{ oops }";
        Result<ModManifest, ManifestError> r = ManifestParser.parse(json);
        assertThat(r.isErr()).isTrue();
        assertThat(r.unwrapErr()).isInstanceOf(ManifestError.SyntaxError.class);
    }

    @Test
    void rejectsInvalidIdPattern() {
        String json = """
                { "schema":1, "id":"Upper_Case", "version":"1.0.0", "name":"X" }
                """;
        Result<ModManifest, ManifestError> r = ManifestParser.parse(json);
        assertThat(r.isErr()).isTrue();
        assertThat(r.unwrapErr()).isInstanceOf(ManifestError.InvalidValue.class);
    }

    @Test
    void ignoresUnknownTopLevelFields() {
        String json = """
                {
                  "schema":1, "id":"x", "version":"1.0.0", "name":"X",
                  "future_field_we_dont_know": { "anything": [1,2,3] }
                }
                """;
        Result<ModManifest, ManifestError> r = ManifestParser.parse(json);
        assertThat(r.isOk()).isTrue();
    }
}
