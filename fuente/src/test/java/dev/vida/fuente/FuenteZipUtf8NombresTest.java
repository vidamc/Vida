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

class FuenteZipUtf8NombresTest {

    @Test
    void nombres_entrada_no_ascii() throws Exception {
        ModManifest manifest = ModManifest.builder("demo_utf", Version.of(1, 0, 0), "Demo")
                .custom(java.util.Map.of("vida:dataDriven", java.util.Map.of(
                        "enabled", true,
                        "datapackRoot", "data/demo_utf/vida")))
                .build();
        String entry = "data/demo_utf/vida/bloques/камень.json";
        String body = """
                {"id":"demo_utf:roca","material":"PIEDRA","dureza":1.0}
                """;
        byte[] zip = zip(Map.of(entry, body));
        try (BytesZipReader r = new BytesZipReader("démo.jar", zip)) {
            Result<FuenteContenidoMod, FuenteError> p = FuentePrototipoParser.leer(manifest, r);
            assertThat(p.isOk()).isTrue();
            assertThat(p.unwrap().bloques()).hasSize(1);
        }
    }

    private static byte[] zip(Map<String, String> entries) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> e : new LinkedHashMap<>(entries).entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
