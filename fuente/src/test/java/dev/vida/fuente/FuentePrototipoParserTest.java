/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.fuente;

import static org.assertj.core.api.Assertions.assertThat;

import dev.vida.core.Result;
import dev.vida.core.Version;
import dev.vida.discovery.BytesZipReader;
import dev.vida.manifest.ModManifest;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class FuentePrototipoParserTest {

    @Test
    void lee_bloques_objetos_y_recetas_shaped() throws Exception {
        ModManifest manifest = ModManifest.builder("demo", Version.of(1, 0, 0), "Demo")
                .custom(Map.of("vida:dataDriven", Map.of(
                        "enabled", true,
                        "datapackRoot", "data/demo/vida")))
                .build();

        byte[] zipBytes = zip(Map.of(
                "data/demo/vida/bloques/piedra.json",
                """
                {"id":"demo:piedra_lisa","material":"PIEDRA","dureza":2.5}
                """,
                "data/demo/vida/objetos/gema.json",
                """
                {"id":"demo:gema_azul","tipo":"MATERIAL","maxPila":32}
                """,
                "data/demo/vida/recipes/gema_bloque.json",
                """
                {
                  "type":"vida:shaped",
                  "pattern":["AA","AA"],
                  "key":{"A":"demo:gema_azul"},
                  "result":{"id":"demo:bloque_gema","count":1}
                }
                """));

        try (BytesZipReader zip = new BytesZipReader("demo.jar", zipBytes)) {
            Result<FuenteContenidoMod, FuenteError> parsed = FuentePrototipoParser.leer(manifest, zip);
            assertThat(parsed.isOk()).isTrue();

            FuenteContenidoMod contenido = parsed.unwrap();
            assertThat(contenido.habilitado()).isTrue();
            assertThat(contenido.bloques()).hasSize(1);
            assertThat(contenido.objetos()).hasSize(1);
            assertThat(contenido.recetasShaped()).hasSize(1);
            assertThat(contenido.recetasShaped().get(0).resultado().id().toString())
                    .isEqualTo("demo:bloque_gema");

            Result<FuenteContenidoMod, FuenteError> again = FuentePrototipoParser.leer(manifest, zip);
            assertThat(again.isOk()).isTrue();
            assertThat(again.unwrap()).isEqualTo(contenido);
        }
    }

    @Test
    void retorna_deshabilitado_si_config_falta() throws Exception {
        ModManifest manifest = ModManifest.builder("demo", Version.of(1, 0, 0), "Demo").build();
        try (BytesZipReader zip = new BytesZipReader("empty.jar", zip(Map.of()))) {
            Result<FuenteContenidoMod, FuenteError> parsed = FuentePrototipoParser.leer(manifest, zip);
            assertThat(parsed.isOk()).isTrue();
            assertThat(parsed.unwrap()).isEqualTo(FuenteContenidoMod.DESHABILITADO);
        }
    }

    @Test
    void lee_loot_table_pools() throws Exception {
        ModManifest manifest = ModManifest.builder("demo", Version.of(1, 0, 0), "Demo")
                .custom(Map.of("vida:dataDriven", Map.of(
                        "enabled", true,
                        "datapackRoot", "data/demo/vida")))
                .build();
        byte[] zipBytes = zip(Map.of(
                "data/demo/vida/loot_tables/blocks/terron.json",
                """
                {
                  "type": "minecraft:generic",
                  "pools": [
                    {
                      "rolls": 1,
                      "entries": [
                        { "type": "minecraft:item", "name": "demo:semilla" }
                      ]
                    }
                  ]
                }
                """));
        try (BytesZipReader zip = new BytesZipReader("demo.jar", zipBytes)) {
            Result<FuenteContenidoMod, FuenteError> parsed = FuentePrototipoParser.leer(manifest, zip);
            assertThat(parsed.isOk()).isTrue();
            FuenteContenidoMod c = parsed.unwrap();
            assertThat(c.tablasLoot()).hasSize(1);
            assertThat(c.tablasLoot().get(0).id().toString()).isEqualTo("demo:blocks/terron");
            assertThat(c.tablasLoot().get(0).itemsExtraidos().get(0).toString())
                    .isEqualTo("demo:semilla");
            assertThat(c.tablasLoot().get(0).tablasReferenciadas()).isEmpty();
        }
    }

    @Test
    void loot_table_anidada_ok_si_json_referenciado_presente() throws Exception {
        ModManifest manifest = ModManifest.builder("demo", Version.of(1, 0, 0), "Demo")
                .custom(Map.of("vida:dataDriven", Map.of(
                        "enabled", true,
                        "datapackRoot", "data/demo/vida")))
                .build();
        byte[] zipBytes = zip(Map.of(
                "data/demo/vida/loot_tables/blocks/hijo.json",
                """
                {
                  "type": "minecraft:generic",
                  "pools": [{ "rolls": 1, "entries": [
                    { "type": "minecraft:item", "name": "demo:moneda" }
                  ]}]
                }
                """,
                "data/demo/vida/loot_tables/blocks/padre.json",
                """
                {
                  "type": "minecraft:generic",
                  "pools": [{ "rolls": 1, "entries": [
                    { "type": "minecraft:loot_table", "name": "demo:blocks/hijo" }
                  ]}]
                }
                """));
        try (BytesZipReader zip = new BytesZipReader("demo.jar", zipBytes)) {
            Result<FuenteContenidoMod, FuenteError> parsed = FuentePrototipoParser.leer(manifest, zip);
            assertThat(parsed.isOk()).isTrue();
            FuenteContenidoMod c = parsed.unwrap();
            assertThat(c.tablasLoot()).hasSize(2);
            FuenteLootTable padre = c.tablasLoot().stream()
                    .filter(t -> t.id().toString().equals("demo:blocks/padre"))
                    .findFirst()
                    .orElseThrow();
            assertThat(padre.tablasReferenciadas().get(0).toString()).isEqualTo("demo:blocks/hijo");
        }
    }

    @Test
    void loot_table_anidada_falla_si_referencia_sin_json() throws Exception {
        ModManifest manifest = ModManifest.builder("demo", Version.of(1, 0, 0), "Demo")
                .custom(Map.of("vida:dataDriven", Map.of(
                        "enabled", true,
                        "datapackRoot", "data/demo/vida")))
                .build();
        byte[] zipBytes = zip(Map.of(
                "data/demo/vida/loot_tables/blocks/padre.json",
                """
                {
                  "type": "minecraft:generic",
                  "pools": [{ "rolls": 1, "entries": [
                    { "type": "minecraft:loot_table", "name": "demo:blocks/no_existe" }
                  ]}]
                }
                """));
        try (BytesZipReader zip = new BytesZipReader("demo.jar", zipBytes)) {
            Result<FuenteContenidoMod, FuenteError> parsed = FuentePrototipoParser.leer(manifest, zip);
            assertThat(parsed.isErr()).isTrue();
            assertThat(parsed.unwrapErr()).isInstanceOf(FuenteError.TablaLootReferenciaRota.class);
        }
    }

    @Test
    void worldgen_coincide_con_loot_y_bloque_fuente() throws Exception {
        ModManifest manifest = ModManifest.builder("demo", Version.of(1, 0, 0), "Demo")
                .custom(Map.of("vida:dataDriven", Map.of(
                        "enabled", true,
                        "datapackRoot", "data/demo/vida")))
                .build();
        byte[] zipBytes = zip(Map.of(
                "data/demo/vida/bloques/roca.json",
                """
                {"id":"demo:roca","material":"PIEDRA","dureza":1.1}
                """,
                "data/demo/vida/loot_tables/chests/caja.json",
                """
                {
                  "type": "minecraft:generic",
                  "pools": [{ "rolls": 1, "entries": [
                    { "type": "minecraft:item", "name": "minecraft:stick" }
                  ]}]
                }
                """,
                "data/demo/vida/worldgen/placed_feature/outcrop.json",
                """
                {
                  "feature": {
                    "type": "minecraft:vegetation_patch",
                    "vegetation_patch": {}
                  },
                  "loot_anchor": { "loot_table": "demo:chests/caja" },
                  "surface": { "Name": "demo:roca", "Properties": {} }
                }
                """));
        try (BytesZipReader zip = new BytesZipReader("demo.jar", zipBytes)) {
            Result<FuenteContenidoMod, FuenteError> parsed = FuentePrototipoParser.leer(manifest, zip);
            assertThat(parsed.isOk()).isTrue();
            FuenteContenidoMod c = parsed.unwrap();
            assertThat(c.worldgen()).hasSize(1);
            assertThat(c.worldgen().get(0).refsLoot().get(0).toString()).isEqualTo("demo:chests/caja");
            assertThat(c.worldgen().get(0).refsBloque().get(0).toString()).isEqualTo("demo:roca");
        }
    }

    @Test
    void worldgen_falla_si_loot_mod_no_declarado() throws Exception {
        ModManifest manifest = ModManifest.builder("demo", Version.of(1, 0, 0), "Demo")
                .custom(Map.of("vida:dataDriven", Map.of(
                        "enabled", true,
                        "datapackRoot", "data/demo/vida")))
                .build();
        byte[] zipBytes = zip(Map.of(
                "data/demo/vida/worldgen/processor_list/x.json",
                """
                { "processors": [ { "loot_table": "demo:no_existe" } ] }
                """));
        try (BytesZipReader zip = new BytesZipReader("demo.jar", zipBytes)) {
            Result<FuenteContenidoMod, FuenteError> parsed = FuentePrototipoParser.leer(manifest, zip);
            assertThat(parsed.isErr()).isTrue();
            assertThat(parsed.unwrapErr()).isInstanceOf(FuenteError.WorldgenLootReferenciaRota.class);
        }
    }

    @Test
    void worldgen_falla_si_bloque_mod_no_declarado() throws Exception {
        ModManifest manifest = ModManifest.builder("demo", Version.of(1, 0, 0), "Demo")
                .custom(Map.of("vida:dataDriven", Map.of(
                        "enabled", true,
                        "datapackRoot", "data/demo/vida")))
                .build();
        byte[] zipBytes = zip(Map.of(
                "data/demo/vida/worldgen/configured_feature/y.json",
                """
                { "state": { "Name": "demo:falta", "Properties": {} } }
                """));
        try (BytesZipReader zip = new BytesZipReader("demo.jar", zipBytes)) {
            Result<FuenteContenidoMod, FuenteError> parsed = FuentePrototipoParser.leer(manifest, zip);
            assertThat(parsed.isErr()).isTrue();
            assertThat(parsed.unwrapErr()).isInstanceOf(FuenteError.WorldgenBloqueReferenciaRoto.class);
        }
    }

    @Test
    void worldgen_no_exige_loot_minecraft() throws Exception {
        ModManifest manifest = ModManifest.builder("demo", Version.of(1, 0, 0), "Demo")
                .custom(Map.of("vida:dataDriven", Map.of(
                        "enabled", true,
                        "datapackRoot", "data/demo/vida")))
                .build();
        byte[] zipBytes = zip(Map.of(
                "data/demo/vida/worldgen/structure/z.json",
                """
                { "minecraft:chests": { "loot_table": "minecraft:chests/spawn_bonus_chest" } }
                """));
        try (BytesZipReader zip = new BytesZipReader("demo.jar", zipBytes)) {
            Result<FuenteContenidoMod, FuenteError> parsed = FuentePrototipoParser.leer(manifest, zip);
            assertThat(parsed.isOk()).isTrue();
            assertThat(parsed.unwrap().worldgen().get(0).refsLoot().get(0).namespace())
                    .isEqualTo("minecraft");
        }
    }

    @Test
    void reporta_error_si_recipe_no_es_shaped() throws Exception {
        ModManifest manifest = ModManifest.builder("demo", Version.of(1, 0, 0), "Demo")
                .custom(Map.of("vida:dataDriven", Map.of("enabled", true)))
                .build();
        byte[] zipBytes = zip(Map.of(
                "data/demo/vida/recipes/bad.json",
                """
                {"type":"minecraft:smelting"}
                """));

        try (BytesZipReader zip = new BytesZipReader("demo.jar", zipBytes)) {
            Result<FuenteContenidoMod, FuenteError> parsed = FuentePrototipoParser.leer(manifest, zip);
            assertThat(parsed.isErr()).isTrue();
            assertThat(parsed.unwrapErr()).isInstanceOf(FuenteError.JsonInvalido.class);
        }
    }

    private static byte[] zip(Map<String, String> entries) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : new LinkedHashMap<>(entries).entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
