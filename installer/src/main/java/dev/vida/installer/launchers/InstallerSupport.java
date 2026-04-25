/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.launchers;

import dev.vida.installer.InstallerCore;
import dev.vida.installer.mc.McArtifacts;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Общие утилиты, которые переиспользуют {@link LauncherHandler}'ы:
 * извлечение «вшитого» loader.jar, атомарная запись файлов, OS-детект
 * и пр.
 *
 * <p>Все методы — static, класс не инстанцируется.
 */
public final class InstallerSupport {

    private InstallerSupport() {}

    /**
     * Как {@link #extractEmbeddedLoader(Path, boolean, Path)} с каталогом кэша из
     * переменной окружения {@code VIDA_INSTALL_CACHE} или свойства JVM
     * {@code vida.install.cache}.
     */
    public static McArtifacts.Sha1Result extractEmbeddedLoader(Path target, boolean dryRun)
            throws IOException {
        return extractEmbeddedLoader(target, dryRun, resolverCacheDir());
    }

    /**
     * Извлекает embedded {@code /loader/loader.jar}. Если задан {@code cacheDir} и там уже
     * есть файл с тем же SHA-1, что у ресурса, выполняется копирование из кэша (offline).
     */
    public static McArtifacts.Sha1Result extractEmbeddedLoader(Path target, boolean dryRun,
            Path cacheDir)
            throws IOException {
        McArtifacts.Sha1Result fingerprint;
        try (InputStream in = openLoaderStream()) {
            fingerprint = McArtifacts.copyWithSha1(in, OutputStream.nullOutputStream());
        }

        if (!dryRun && cacheDir != null) {
            Files.createDirectories(cacheDir);
            Path cached = cacheDir.resolve("loader-" + fingerprint.sha1Hex() + ".jar");
            if (Files.isRegularFile(cached)) {
                McArtifacts.Sha1Result disk = McArtifacts.sha1Of(cached);
                if (disk.sha1Hex().equalsIgnoreCase(fingerprint.sha1Hex())) {
                    Files.createDirectories(target.getParent());
                    Files.copy(cached, target, StandardCopyOption.REPLACE_EXISTING);
                    return McArtifacts.sha1Of(target);
                }
            }
        }

        try (InputStream in = openLoaderStream()) {
            if (dryRun) {
                return McArtifacts.copyWithSha1(in, OutputStream.nullOutputStream());
            }
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".part");
            McArtifacts.Sha1Result result;
            try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(tmp))) {
                result = McArtifacts.copyWithSha1(in, out);
            }
            try {
                Files.move(tmp, target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFail) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }

            if (cacheDir != null && Files.isRegularFile(target)) {
                try {
                    Files.createDirectories(cacheDir);
                    Path cached = cacheDir.resolve("loader-" + fingerprint.sha1Hex() + ".jar");
                    Files.copy(target, cached, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignore) {
                    // cache is best-effort
                }
            }
            return result;
        }
    }

    private static Path resolverCacheDir() {
        String env = System.getenv("VIDA_INSTALL_CACHE");
        if (env != null && !env.isBlank()) {
            return Path.of(env.trim());
        }
        String prop = System.getProperty("vida.install.cache", "");
        return prop.isBlank() ? null : Path.of(prop.trim());
    }

    private static InputStream openLoaderStream() throws IOException {
        InputStream in = InstallerCore.class
                .getResourceAsStream(InstallerCore.EMBEDDED_LOADER_RESOURCE);
        if (in == null) {
            throw new IOException(
                    "Embedded loader resource missing: " + InstallerCore.EMBEDDED_LOADER_RESOURCE
                    + ". The installer jar was built incorrectly.");
        }
        return in;
    }

    /**
     * Атомарная запись текстового файла (через {@code .tmp} + rename).
     * Родительские каталоги создаются автоматически.
     */
    public static void writeAtomically(Path target, String content) throws IOException {
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFail) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** {@code true} если запущены на Windows. */
    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
