/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.discovery.internal;

import dev.vida.core.ApiStatus;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Утилиты подсчёта SHA-256 отпечатков. Используются для стабильной
 * идентификации jar-файлов в {@code mods.idx}.
 *
 * <p>Класс ориентирован на минимальные аллокации: буфер для file-based хеша
 * фиксированный.
 */
@ApiStatus.Internal
public final class Fingerprints {

    /** Длина SHA-256 digest в байтах. */
    public static final int SHA256_LENGTH = 32;

    private static final int IO_BUFFER_SIZE = 64 * 1024;

    private Fingerprints() {}

    /** Возвращает SHA-256 байтового массива. */
    public static byte[] sha256(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        MessageDigest digest = newDigest();
        digest.update(bytes);
        return digest.digest();
    }

    /** Возвращает SHA-256 содержимого файла; не читает файл целиком в память. */
    public static byte[] sha256(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        MessageDigest digest = newDigest();
        try (InputStream in = Files.newInputStream(file);
             DigestInputStream dis = new DigestInputStream(in, digest)) {
            byte[] buf = new byte[IO_BUFFER_SIZE];
            while (dis.read(buf) >= 0) {
                // чтение сбрасывает байты в digest
            }
        }
        return digest.digest();
    }

    /** Hex-представление digest-а в нижнем регистре. */
    public static String hex(byte[] digest) {
        Objects.requireNonNull(digest, "digest");
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            int v = b & 0xff;
            sb.append(Character.forDigit(v >>> 4, 16));
            sb.append(Character.forDigit(v & 0x0f, 16));
        }
        return sb.toString();
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
    }
}
