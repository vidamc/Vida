/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.mc;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static MessageDigest sha1() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }
}
