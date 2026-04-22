/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.internal;

import dev.vida.cartografia.ClassMapping;
import dev.vida.cartografia.MappingTree;
import dev.vida.cartografia.Namespace;
import dev.vida.cartografia.io.ProguardReader;
import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import dev.vida.core.Result;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Загружает Mojang client mappings и строит карту
 * obfuscated → deobfuscated class names (internal format).
 *
 * <p>Порядок поиска маппингов:
 * <ol>
 *   <li>{@code .minecraft/versions/<ver>/client_mappings.txt}</li>
 *   <li>Любой {@code *.txt} в {@code .minecraft/versions/<ver>/} размером &gt; 100 КБ
 *       и содержащий ProGuard-маркер {@code " -> "} в первых строках.</li>
 *   <li>Скачивание по URL из version JSON (если файл не найден локально).</li>
 * </ol>
 */
@ApiStatus.Internal
public final class MappingLoader {

    private static final Log LOG = Log.of(MappingLoader.class);

    private MappingLoader() {}

    /**
     * Пытается загрузить маппинги и вернуть карту obf→deobf internal class names.
     *
     * @param mcVersion  версия Minecraft (например, {@code "1.21.1"})
     * @param gameDir    корень {@code .minecraft} (содержит {@code versions/})
     * @return карту или пустую Map, если маппинги не удалось загрузить
     */
    public static Map<String, String> loadClassMap(String mcVersion, Path gameDir) {
        if (mcVersion == null || gameDir == null) return Map.of();

        Path versionsDir = gameDir.resolve("versions").resolve(mcVersion);
        if (!Files.isDirectory(versionsDir)) {
            LOG.warn("Vida: versions dir not found: {}", versionsDir);
            return Map.of();
        }

        Optional<Path> mappingFile = findMappingsFile(versionsDir, mcVersion);
        if (mappingFile.isEmpty()) {
            Optional<String> url = readMappingsUrl(versionsDir, mcVersion);
            if (url.isPresent()) {
                mappingFile = downloadMappings(url.get(), versionsDir);
            }
        }

        if (mappingFile.isEmpty()) {
            LOG.warn("Vida: client mappings not found for MC {}", mcVersion);
            return Map.of();
        }

        return parseMappings(mappingFile.get());
    }

    private static Optional<Path> findMappingsFile(Path versionsDir, String mcVersion) {
        // Standard name used by launchers
        Path standard = versionsDir.resolve("client_mappings.txt");
        if (Files.isRegularFile(standard)) return Optional.of(standard);

        // Alternative names
        Path alt1 = versionsDir.resolve(mcVersion + "-client.txt");
        if (Files.isRegularFile(alt1)) return Optional.of(alt1);

        Path alt2 = versionsDir.resolve("client.txt");
        if (Files.isRegularFile(alt2)) return Optional.of(alt2);

        // Scan for any large .txt file that looks like ProGuard mappings
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(versionsDir, "*.txt")) {
            for (Path p : ds) {
                if (Files.size(p) > 100_000 && looksLikeProguard(p)) {
                    return Optional.of(p);
                }
            }
        } catch (IOException ignored) {}

        return Optional.empty();
    }

    private static boolean looksLikeProguard(Path file) {
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            for (int i = 0; i < 20; i++) {
                String line = reader.readLine();
                if (line == null) break;
                if (line.contains(" -> ") && line.endsWith(":")) return true;
            }
        } catch (IOException ignored) {}
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Optional<String> readMappingsUrl(Path versionsDir, String mcVersion) {
        Path versionJson = versionsDir.resolve(mcVersion + ".json");
        if (!Files.isRegularFile(versionJson)) return Optional.empty();

        try {
            String json = Files.readString(versionJson, StandardCharsets.UTF_8);
            String url = extractNestedJsonString(json, "client_mappings", "url");
            return Optional.ofNullable(url);
        } catch (IOException ex) {
            LOG.warn("Vida: failed to read version JSON: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static Optional<Path> downloadMappings(String urlStr, Path destDir) {
        LOG.info("Vida: downloading client mappings from {}", urlStr);
        try {
            URL url = URI.create(urlStr).toURL();
            Path dest = destDir.resolve("client_mappings.txt");
            try (var in = url.openStream()) {
                Files.copy(in, dest);
            }
            return Optional.of(dest);
        } catch (Exception ex) {
            LOG.warn("Vida: failed to download mappings: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private static Map<String, String> parseMappings(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            var result = ProguardReader.read(file.toString(), reader,
                    Namespace.OBF, Namespace.MOJMAP);
            if (result.isErr()) {
                LOG.warn("Vida: failed to parse mappings: {}", result.unwrapErr());
                return Map.of();
            }
            MappingTree tree = result.unwrap();
            Map<String, String> map = new HashMap<>(tree.size() * 2);
            for (ClassMapping cm : tree.classes()) {
                String obf = cm.name(Namespace.OBF);
                String deobf = cm.name(Namespace.MOJMAP);
                if (obf != null && deobf != null && !obf.equals(deobf)) {
                    map.put(obf, deobf);
                }
            }
            LOG.info("Vida: loaded {} class mappings from {}", map.size(), file.getFileName());
            return Collections.unmodifiableMap(map);
        } catch (IOException ex) {
            LOG.warn("Vida: I/O error reading mappings: {}", ex.getMessage());
            return Map.of();
        }
    }

    /**
     * Minimal extractor for nested JSON: finds {@code "outerKey": { ... "innerKey": "value" }}.
     */
    static String extractNestedJsonString(String json, String outerKey, String innerKey) {
        String outerNeedle = "\"" + outerKey + "\"";
        int oi = json.indexOf(outerNeedle);
        if (oi < 0) return null;
        int braceOpen = json.indexOf('{', oi + outerNeedle.length());
        if (braceOpen < 0) return null;
        int braceClose = json.indexOf('}', braceOpen);
        if (braceClose < 0) return null;
        String inner = json.substring(braceOpen, braceClose + 1);
        return SyntheticProviders.extractJsonString(inner, innerKey);
    }
}
