/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader.internal;

import dev.vida.core.ApiStatus;
import dev.vida.core.Log;
import dev.vida.core.Version;
import dev.vida.loader.BootOptions;
import dev.vida.resolver.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Конструирует «платформенные» {@link Provider}-ы, которые добавляются в
 * {@code Universe} резолвера перед запуском: {@code vida}, {@code minecraft},
 * {@code java}.
 *
 * <p>Без этих провайдеров любой мод с зависимостью
 * {@code "required": { "vida": ">=0.7" }} (или {@code "minecraft"} /
 * {@code "java"}) получает {@link dev.vida.resolver.ResolverError.Missing}.
 * Синтетика закрывает этот пробел, не навязывая моду знаний о конкретном
 * загрузчике.
 *
 * <h2>Источники версий</h2>
 * <ul>
 *   <li><b>vida</b> — {@link BootOptions#vidaVersion()} или встроенный ресурс
 *       {@code META-INF/vida/loader-version.properties} (ключ {@code version}),
 *       который заполняется процессом сборки из корневого {@code version.txt}.</li>
 *   <li><b>minecraft</b> — только {@link BootOptions#minecraftVersion()}.
 *       Если версия не задана — провайдер пропускается (моды с
 *       {@code required.minecraft} осознанно падают с Missing, указывая на
 *       недостающую настройку лончера).</li>
 *   <li><b>java</b> — {@code System.getProperty("java.specification.version")};
 *       "21" канонизируется в "21.0.0".</li>
 * </ul>
 *
 * <h2>Коллизии с реальными модами</h2>
 * Если среди кандидатов есть мод с {@code id} = {@code vida} / {@code minecraft} /
 * {@code java}, синтетический провайдер для этого id пропускается — реальный
 * мод выигрывает. Это позволяет при необходимости подменять платформенные
 * идентификаторы модами-заглушками в тестовых сборках.
 */
@ApiStatus.Internal
public final class SyntheticProviders {

    private static final Log LOG = Log.of(SyntheticProviders.class);

    /**
     * Ресурс с версией загрузчика. Заполняется {@code processResources}
     * через expand-фильтр (см. {@code loader/build.gradle.kts}).
     */
    public static final String LOADER_VERSION_RESOURCE =
            "META-INF/vida/loader-version.properties";

    /** Консервативный fallback, если ресурс потерялся (например, в голом classpath теста). */
    static final Version FALLBACK_VIDA_VERSION = Version.of(0, 0, 0);

    private SyntheticProviders() {}

    /**
     * Собирает список провайдеров, которых следует добавить в {@code Universe}.
     *
     * @param options       параметры бутстрапа
     * @param existingIds   id реальных мод-кандидатов; для них синтетика не публикуется
     * @return плоский список провайдеров, готовых к {@code Universe.Builder#add}
     */
    public static List<Provider> build(BootOptions options, Set<String> existingIds) {
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(existingIds, "existingIds");

        List<Provider> out = new ArrayList<>(3);

        if (!existingIds.contains("vida")) {
            Version v = resolveVidaVersion(options);
            out.add(Provider.builder("vida", v).build());
            LOG.info("Vida synthetic provider: vida={}", v);
        }
        if (!existingIds.contains("minecraft")) {
            Optional<Version> mc = resolveMinecraftVersion(options);
            mc.ifPresent(v -> {
                out.add(Provider.builder("minecraft", v).build());
                LOG.info("Vida synthetic provider: minecraft={}", v);
            });
        }
        if (!existingIds.contains("java")) {
            Version j = resolveJavaVersion();
            out.add(Provider.builder("java", j).build());
            LOG.info("Vida synthetic provider: java={}", j);
        }
        return out;
    }

    // =================================================================
    //                        version resolution
    // =================================================================

    static Version resolveVidaVersion(BootOptions options) {
        Optional<Version> fromOptions = options.vidaVersion().flatMap(SyntheticProviders::tryCanonicalize);
        if (fromOptions.isPresent()) return fromOptions.get();
        return readBundledVidaVersion().orElse(FALLBACK_VIDA_VERSION);
    }

    static Optional<Version> resolveMinecraftVersion(BootOptions options) {
        Optional<Version> explicit = options.minecraftVersion().flatMap(SyntheticProviders::tryCanonicalize);
        if (explicit.isPresent()) return explicit;
        return detectMinecraftVersionFromClasspath();
    }

    /**
     * Minecraft client JAR contains {@code version.json} at the root with
     * {@code {"id":"1.21.1", ...}}. If the game is on the classpath we can
     * read this file and extract the version automatically.
     */
    private static Optional<Version> detectMinecraftVersionFromClasspath() {
        try {
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            try (InputStream in = cl.getResourceAsStream("version.json")) {
                if (in == null) return Optional.empty();
                String json = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                String id = extractJsonString(json, "id");
                if (id == null) return Optional.empty();
                LOG.info("Vida: auto-detected minecraft version '{}' from classpath version.json", id);
                return tryCanonicalize(id);
            }
        } catch (Exception ex) {
            LOG.warn("Vida: failed to detect minecraft version from classpath ({})", ex.toString());
            return Optional.empty();
        }
    }

    /**
     * Minimal JSON string field extractor — avoids pulling a JSON library
     * into the boot path. Looks for {@code "key" : "value"} in the input.
     */
    static String extractJsonString(String json, String key) {
        String needle = "\"" + key + "\"";
        int ki = json.indexOf(needle);
        if (ki < 0) return null;
        int colon = json.indexOf(':', ki + needle.length());
        if (colon < 0) return null;
        int open = json.indexOf('"', colon + 1);
        if (open < 0) return null;
        int close = json.indexOf('"', open + 1);
        if (close < 0) return null;
        return json.substring(open + 1, close);
    }

    static Version resolveJavaVersion() {
        String raw = System.getProperty("java.specification.version", "21");
        return tryCanonicalize(raw).orElseGet(() -> Version.of(21, 0, 0));
    }

    /**
     * Читает встроенный ресурс с версией платформы. Возвращает пустой
     * {@link Optional}, если ресурс отсутствует или повреждён — в таком
     * случае вызывающая сторона должна использовать fallback.
     */
    static Optional<Version> readBundledVidaVersion() {
        ClassLoader cl = SyntheticProviders.class.getClassLoader();
        if (cl == null) cl = ClassLoader.getSystemClassLoader();
        try (InputStream in = cl.getResourceAsStream(LOADER_VERSION_RESOURCE)) {
            if (in == null) return Optional.empty();
            Properties p = new Properties();
            p.load(in);
            String raw = p.getProperty("version");
            if (raw == null || raw.isBlank()) return Optional.empty();
            return tryCanonicalize(raw);
        } catch (IOException ex) {
            LOG.warn("Vida: failed to read bundled loader version ({})", ex.toString());
            return Optional.empty();
        }
    }

    // =================================================================
    //                       version canonicalization
    // =================================================================

    /**
     * Приводит произвольный строковый ярлык версии к полноформатному
     * SemVer: "21" → 21.0.0, "1.21" → 1.21.0, "1.21.1" → как есть.
     *
     * <p>Pre-release / build-метаданные сохраняются, если они уже валидны.
     * Невалидные входы дают {@link Optional#empty()}.
     */
    static Optional<Version> tryCanonicalize(String raw) {
        if (raw == null) return Optional.empty();
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return Optional.empty();

        // Если уже валидная — принимаем как есть.
        Optional<Version> direct = Version.tryParse(trimmed);
        if (direct.isPresent()) return direct;

        // Выделяем core (major.minor.patch) и суффиксы pre-release/build.
        String core = trimmed;
        String suffix = "";
        int cut = indexOfAny(trimmed, '-', '+');
        if (cut > 0) {
            core = trimmed.substring(0, cut);
            suffix = trimmed.substring(cut);
        }

        String[] parts = core.split("\\.", -1);
        if (parts.length == 0 || parts.length > 3) return Optional.empty();
        int[] nums = new int[3];
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty() || !isDigits(p)) return Optional.empty();
            // Leading zeros запрещены SemVer, но "01" и т. п. от java
            // не возникают — нормализуем через Integer.parseInt.
            try {
                nums[i] = Integer.parseInt(p);
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
        }
        return Version.tryParse(nums[0] + "." + nums[1] + "." + nums[2] + suffix);
    }

    private static int indexOfAny(String s, char a, char b) {
        int ia = s.indexOf(a);
        int ib = s.indexOf(b);
        if (ia < 0) return ib;
        if (ib < 0) return ia;
        return Math.min(ia, ib);
    }

    private static boolean isDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return !s.isEmpty();
    }
}
