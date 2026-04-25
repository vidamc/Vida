/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModScannerTest {

    private static final String SIMPLE_MANIFEST = """
            {
              "schema": 1,
              "id": "alpha",
              "version": "1.0.0",
              "name": "Alpha"
            }
            """;

    private static final String NESTED_MANIFEST = """
            {
              "schema": 1,
              "id": "alpha",
              "version": "1.0.0",
              "name": "Alpha",
              "custom": {
                "jars": ["META-INF/jars/inner.jar"]
              }
            }
            """;

    private static final String INNER_MANIFEST = """
            {
              "schema": 1,
              "id": "beta",
              "version": "2.0.0",
              "name": "Beta"
            }
            """;

    // --------------------------------------------------------------- cases

    @Test
    void emptyDirectoryProducesEmptyReport(@TempDir Path tmp) {
        DiscoveryReport report = ModScanner.scan(tmp);
        assertThat(report.size()).isZero();
        assertThat(report.errors()).isEmpty();
    }

    @Test
    void missingDirectoryYieldsError(@TempDir Path tmp) {
        Path missing = tmp.resolve("nope");
        DiscoveryReport report = ModScanner.scan(missing);
        assertThat(report.size()).isZero();
        assertThat(report.errors()).singleElement().isInstanceOf(DiscoveryError.NotADirectory.class);
    }

    @Test
    void simpleJarIsDiscovered(@TempDir Path tmp) throws IOException {
        Path jar = tmp.resolve("alpha.jar");
        TestJars.writeToDisk(jar, entries("vida.mod.json", SIMPLE_MANIFEST));

        DiscoveryReport report = ModScanner.scan(tmp);
        assertThat(report.errors()).isEmpty();
        assertThat(report.topLevel()).hasSize(1);
        ModCandidate c = report.topLevel().get(0);
        assertThat(c.id()).isEqualTo("alpha");
        assertThat(c.depth()).isZero();
        assertThat(c.nested()).isEmpty();
        assertThat(c.sha256Hex()).matches("[0-9a-f]{64}");
    }

    @Test
    void missingManifestIsReported(@TempDir Path tmp) throws IOException {
        Path jar = tmp.resolve("bogus.jar");
        TestJars.writeToDisk(jar, entries("README.md", "hello"));

        DiscoveryReport report = ModScanner.scan(tmp);
        assertThat(report.topLevel()).isEmpty();
        assertThat(report.errors()).singleElement().isInstanceOf(DiscoveryError.ManifestMissing.class);
    }

    @Test
    void badManifestIsReported(@TempDir Path tmp) throws IOException {
        Path jar = tmp.resolve("bad.jar");
        TestJars.writeToDisk(jar, entries("vida.mod.json", "{ not-valid"));

        DiscoveryReport report = ModScanner.scan(tmp);
        assertThat(report.topLevel()).isEmpty();
        assertThat(report.errors()).singleElement().isInstanceOf(DiscoveryError.ManifestParse.class);
    }

    @Test
    void nestedJarIsDiscovered(@TempDir Path tmp) throws IOException {
        byte[] innerJar = TestJars.buildBytes(toByteMap(entries("vida.mod.json", INNER_MANIFEST)));

        Map<String, byte[]> outer = new LinkedHashMap<>();
        outer.put("vida.mod.json", TestJars.utf8(NESTED_MANIFEST));
        outer.put("META-INF/jars/inner.jar", innerJar);

        Path jar = tmp.resolve("outer.jar");
        TestJars.writeToDisk(jar, outer);

        DiscoveryReport report = ModScanner.scan(tmp);
        assertThat(report.errors()).isEmpty();
        assertThat(report.topLevel()).hasSize(1);
        ModCandidate outerC = report.topLevel().get(0);
        assertThat(outerC.nested()).hasSize(1);
        ModCandidate inner = outerC.nested().get(0);
        assertThat(inner.id()).isEqualTo("beta");
        assertThat(inner.depth()).isEqualTo(1);
        assertThat(inner.source()).isInstanceOf(ModSource.Embedded.class);

        // all() включает outer + inner
        assertThat(report.all()).hasSize(2);
    }

    @Test
    void nestedTooDeepIsReported(@TempDir Path tmp) throws IOException {
        // Мод объявил jars, но скан запущен с maxNestingDepth=0.
        byte[] innerJar = TestJars.buildBytes(toByteMap(entries("vida.mod.json", INNER_MANIFEST)));

        Map<String, byte[]> outer = new LinkedHashMap<>();
        outer.put("vida.mod.json", TestJars.utf8(NESTED_MANIFEST));
        outer.put("META-INF/jars/inner.jar", innerJar);

        Path jar = tmp.resolve("outer.jar");
        TestJars.writeToDisk(jar, outer);

        ScanOptions opts = ScanOptions.defaults().withMaxNestingDepth(0);
        DiscoveryReport report = ModScanner.scan(tmp, opts);
        assertThat(report.topLevel()).hasSize(1);
        assertThat(report.topLevel().get(0).nested()).isEmpty();
        assertThat(report.errors())
                .singleElement()
                .isInstanceOf(DiscoveryError.NestingTooDeep.class);
    }

    @Test
    void nestedMissingIsReported(@TempDir Path tmp) throws IOException {
        Map<String, byte[]> outer = entries(
                "vida.mod.json",
                """
                {
                  "schema": 1,
                  "id": "alpha",
                  "version": "1.0.0",
                  "name": "Alpha",
                  "custom": { "jars": ["META-INF/jars/not-there.jar"] }
                }
                """);

        Path jar = tmp.resolve("alpha.jar");
        TestJars.writeToDisk(jar, outer);

        DiscoveryReport report = ModScanner.scan(tmp);
        assertThat(report.topLevel()).hasSize(1);
        assertThat(report.errors()).singleElement().isInstanceOf(DiscoveryError.NestedMissing.class);
    }

    @Test
    void badArchiveIsReported(@TempDir Path tmp) throws IOException {
        Path jar = tmp.resolve("broken.jar");
        Files.write(jar, "not a zip".getBytes(StandardCharsets.UTF_8));

        DiscoveryReport report = ModScanner.scan(tmp);
        assertThat(report.topLevel()).isEmpty();
        assertThat(report.errors()).anySatisfy(
                e -> assertThat(e).isInstanceOfAny(
                        DiscoveryError.BadArchive.class,
                        DiscoveryError.IoError.class));
    }

    @Test
    void fingerprintsCanBeSkipped(@TempDir Path tmp) throws IOException {
        Path jar = tmp.resolve("alpha.jar");
        TestJars.writeToDisk(jar, entries("vida.mod.json", SIMPLE_MANIFEST));

        DiscoveryReport report = ModScanner.scan(tmp,
                ScanOptions.defaults().withComputeFingerprints(false));
        assertThat(report.topLevel().get(0).sha256Hex())
                .isEqualTo("0".repeat(64));
    }

    @Test
    void scan_three_hundred_mods_completes(@TempDir Path tmp) throws IOException {
        for (int i = 0; i < 300; i++) {
            String id = "bulk" + i;
            String manifest = """
                    {
                      "schema": 1,
                      "id": "%s",
                      "version": "1.0.0",
                      "name": "%s"
                    }
                    """.formatted(id, id);
            Path jar = tmp.resolve(id + ".jar");
            TestJars.writeToDisk(jar, entries("vida.mod.json", manifest));
        }
        long t0 = System.nanoTime();
        DiscoveryReport report = ModScanner.scan(tmp);
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        assertThat(report.errors()).isEmpty();
        assertThat(report.size()).isEqualTo(300);
        assertThat(ms).isLessThan(120_000L);
    }

    // --------------------------------------------------------------- helpers

    private static Map<String, byte[]> entries(String path, String content) {
        Map<String, byte[]> m = new LinkedHashMap<>();
        m.put(path, TestJars.utf8(content));
        return m;
    }

    private static Map<String, byte[]> toByteMap(Map<String, byte[]> m) {
        return m;
    }
}
