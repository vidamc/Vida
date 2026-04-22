/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.fuente;

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
