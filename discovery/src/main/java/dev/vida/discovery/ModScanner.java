/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery;

import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import dev.vida.discovery.internal.Fingerprints;
import dev.vida.discovery.internal.NestedJars;
import dev.vida.manifest.ManifestError;
import dev.vida.manifest.ManifestParser;
import dev.vida.manifest.ModManifest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipException;

/**
 * Сканер директории {@code mods/}.
 *
 * <p>Алгоритм:
 * <ol>
 *   <li>Перечислить файлы с нужными расширениями (по умолчанию — {@code .jar}).</li>
 *   <li>Для каждого открыть {@link ZipReader}, вычитать
 *       {@link ScanOptions#manifestPath()} и распарсить через
 *       {@link ManifestParser}.</li>
 *   <li>Если манифест объявляет вложенные JAR
 *       ({@code custom.jars}) — считать их байты и рекурсивно повторить
 *       процедуру до {@link ScanOptions#maxNestingDepth()}.</li>
 *   <li>Посчитать SHA-256 архива (для кэша {@code mods.idx}).</li>
 *   <li>Собрать {@link DiscoveryReport} с успехами и ошибками.</li>
 * </ol>
 *
 * <p>Скан не бросает исключений для ожидаемых проблем — все ошибки
 * фиксируются в {@link DiscoveryError} и возвращаются в отчёте. Это позволяет
 * игре запускаться с частичным набором модов при сбое одного из них.
 */
@ApiStatus.Stable
public final class ModScanner {

    private ModScanner() {}

    /** Скан с настройками по умолчанию. */
    public static DiscoveryReport scan(Path modsDir) {
        return scan(modsDir, ScanOptions.defaults());
    }

    /** Основная точка входа. */
    public static DiscoveryReport scan(Path modsDir, ScanOptions options) {
        Objects.requireNonNull(modsDir, "modsDir");
        Objects.requireNonNull(options, "options");

        long startNs = System.nanoTime();
        List<ModCandidate> topLevel = new ArrayList<>();
        List<DiscoveryError> errors = new ArrayList<>();

        if (!Files.isDirectory(modsDir)) {
            errors.add(new DiscoveryError.NotADirectory(modsDir.toString()));
            return new DiscoveryReport(topLevel, errors, Duration.ofNanos(System.nanoTime() - startNs));
        }

        List<Path> archives = listArchives(modsDir, options.extensions(), errors);
        for (Path path : archives) {
            ModSource src = new ModSource.OnDisk(path);
            ModCandidate c = scanOne(src, options, 0, errors);
            if (c != null) {
                topLevel.add(c);
            }
        }

        return new DiscoveryReport(topLevel, errors, Duration.ofNanos(System.nanoTime() - startNs));
    }

    // =============================================================== private

    private static List<Path> listArchives(
            Path dir, Set<String> extensions, List<DiscoveryError> errors) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> matchesExtension(p, extensions))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            errors.add(new DiscoveryError.IoError(dir.toString(), e.getMessage()));
            return List.of();
        }
    }

    private static boolean matchesExtension(Path p, Set<String> extensions) {
        String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String ext : extensions) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    }

    private static ModCandidate scanOne(
            ModSource src, ScanOptions opts, int depth, List<DiscoveryError> errors) {

        ZipReader reader;
        try {
            reader = src.open();
        } catch (ZipException ze) {
            errors.add(new DiscoveryError.BadArchive(src.id(), ze.getMessage()));
            return null;
        } catch (IOException e) {
            errors.add(new DiscoveryError.IoError(src.id(), e.getMessage()));
            return null;
        }

        try (ZipReader z = reader) {
            String manifestPath = opts.manifestPath();
            if (!z.contains(manifestPath)) {
                errors.add(new DiscoveryError.ManifestMissing(src.id()));
                return null;
            }

            byte[] manifestBytes;
            try {
                manifestBytes = z.read(manifestPath);
            } catch (IOException e) {
                errors.add(new DiscoveryError.IoError(src.id(), e.getMessage()));
                return null;
            }
            String json = new String(manifestBytes, StandardCharsets.UTF_8);

            Result<ModManifest, ManifestError> mr = ManifestParser.parse(json);
            if (mr.isErr()) {
                errors.add(new DiscoveryError.ManifestParse(src.id(), mr.unwrapErr()));
                return null;
            }
            ModManifest manifest = mr.unwrap();

            byte[] sha = opts.computeFingerprints()
                    ? computeFingerprint(src)
                    : emptySha();

            List<ModCandidate> nested = new ArrayList<>();
            if (opts.followNested()) {
                for (String inner : NestedJars.from(manifest)) {
                    ModCandidate child = scanNested(src, z, inner, opts, depth, errors);
                    if (child != null) {
                        nested.add(child);
                    }
                }
            }

            return new ModCandidate(src, manifest, sha, nested, depth);
        }
    }

    private static ModCandidate scanNested(
            ModSource parent, ZipReader parentZip, String innerPath,
            ScanOptions opts, int parentDepth, List<DiscoveryError> errors) {

        int childDepth = parentDepth + 1;
        if (childDepth > opts.maxNestingDepth()) {
            errors.add(new DiscoveryError.NestingTooDeep(
                    parent.id(), childDepth, opts.maxNestingDepth()));
            return null;
        }
        if (!parentZip.contains(innerPath)) {
            errors.add(new DiscoveryError.NestedMissing(parent.id(), innerPath, "entry not found"));
            return null;
        }
        byte[] bytes;
        try {
            bytes = parentZip.read(innerPath);
        } catch (IOException e) {
            errors.add(new DiscoveryError.NestedMissing(parent.id(), innerPath, e.getMessage()));
            return null;
        }
        ModSource nestedSrc = new ModSource.Embedded(parent, innerPath, bytes);
        return scanOne(nestedSrc, opts, childDepth, errors);
    }

    private static byte[] computeFingerprint(ModSource src) {
        try {
            if (src instanceof ModSource.OnDisk od) {
                return Fingerprints.sha256(od.path());
            }
            if (src instanceof ModSource.Embedded em) {
                return Fingerprints.sha256(em.bytes());
            }
            return emptySha();
        } catch (IOException e) {
            // Если не смогли посчитать, возвращаем нули — это валидно для модели,
            // downstream увидит, что отпечаток «не определён». Ошибка в отчёт не
            // поднимается: сам манифест прочитался успешно.
            return emptySha();
        }
    }

    private static byte[] emptySha() {
        return new byte[Fingerprints.SHA256_LENGTH];
    }
}
