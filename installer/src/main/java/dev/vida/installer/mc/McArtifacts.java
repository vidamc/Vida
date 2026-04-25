/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.mc;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Мелкие ФС-утилиты, которыми пользуется {@link dev.vida.installer.InstallerCore}
 * на шаге Minecraft-интеграции.
 */
public final class McArtifacts {

    private McArtifacts() {}

    /** Считает поток, параллельно вычисляя SHA-1 и количество байт. */
    public static Sha1Result copyWithSha1(InputStream in, OutputStream out) throws IOException {
        MessageDigest sha1 = sha1();
        byte[] buf = new byte[16 * 1024];
        long total = 0;
        int n;
        while ((n = in.read(buf)) > 0) {
            sha1.update(buf, 0, n);
            out.write(buf, 0, n);
            total += n;
        }
        return new Sha1Result(HexFormat.of().formatHex(sha1.digest()), total);
    }

    /** SHA-1 существующего файла (без записи). */
    public static Sha1Result sha1Of(Path file) throws IOException {
        MessageDigest sha1 = sha1();
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[16 * 1024];
            long total = 0;
            int n;
            while ((n = in.read(buf)) > 0) {
                sha1.update(buf, 0, n);
                total += n;
            }
            return new Sha1Result(HexFormat.of().formatHex(sha1.digest()), total);
        }
    }

    /**
     * Создаёт пустой jar-маркер для {@code versions/<id>/<id>.jar}.
     * Некоторые лаунчеры (включая MultiMC при импорте кастомных версий)
     * ожидают физический jar-файл даже если он ничего не содержит.
     */
    @SuppressWarnings("try")
    public static void writeEmptyJar(Path target) throws IOException {
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().putValue("Created-By", "vida-installer");
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(target));
             JarOutputStream jar = new JarOutputStream(out, mf)) {
            // интенционально пусто: только META-INF/MANIFEST.MF
        }
    }

    public record Sha1Result(String sha1Hex, long sizeBytes) {}

    /**
     * HTTP(S) GET with resume into {@code target} via a sibling {@code target.part}.
     *
     * <p>If {@code target.part} exists with length {@code N}, sends {@code Range: bytes=N-}.
     * On HTTP 200 while a partial exists (server ignored Range), deletes the partial and retries once.
     * Follows up to 5 redirects via {@code Location}.
     *
     * @return final size in bytes of {@code target}
     */
    public static long downloadHttpResumable(URI uri, Path target) throws IOException {
        Objects.requireNonNull(uri, "uri");
        Objects.requireNonNull(target, "target");
        Path part = target.resolveSibling(target.getFileName() + ".part");
        return downloadHttpResumable(uri, target, part, 0);
    }

    private static long downloadHttpResumable(URI uri, Path target, Path part, int redirects)
            throws IOException {
        if (redirects > 5) {
            throw new IOException("too many redirects for " + uri);
        }
        long start = Files.isRegularFile(part) ? Files.size(part) : 0L;

        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(120_000);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestProperty("User-Agent", "vida-installer-resume/1");
        if (start > 0) {
            conn.setRequestProperty("Range", "bytes=" + start + "-");
        }

        int code = conn.getResponseCode();
        if (code >= 300 && code < 400) {
            String loc = conn.getHeaderField("Location");
            conn.disconnect();
            if (loc == null || loc.isBlank()) {
                throw new IOException("HTTP " + code + " without Location for " + uri);
            }
            URI next = uri.resolve(loc);
            return downloadHttpResumable(next, target, part, redirects + 1);
        }

        if (code == HttpURLConnection.HTTP_OK && start > 0) {
            try (InputStream in = conn.getInputStream()) {
                in.transferTo(OutputStream.nullOutputStream());
            }
            Files.deleteIfExists(part);
            conn.disconnect();
            return downloadHttpResumable(uri, target, part, redirects);
        }

        if (code != HttpURLConnection.HTTP_OK && code != 206) {
            String hint = "";
            try (InputStream err = conn.getErrorStream()) {
                if (err != null) {
                    hint = new String(err.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            conn.disconnect();
            throw new IOException("HTTP " + code + " for " + uri + (hint.isEmpty() ? "" : ": " + hint));
        }

        boolean append = start > 0 && code == 206;
        StandardOpenOption[] opts = append
                ? new StandardOpenOption[] {
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND }
                : new StandardOpenOption[] {
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING };
        try (InputStream in = conn.getInputStream();
                OutputStream out = new BufferedOutputStream(Files.newOutputStream(part, opts))) {
            in.transferTo(out);
        }
        conn.disconnect();

        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try {
            Files.move(part, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFail) {
            Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return Files.size(target);
    }

    private static MessageDigest sha1() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }
}
