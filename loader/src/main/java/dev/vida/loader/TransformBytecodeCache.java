/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.loader;

import dev.vida.core.ApiStatus;
import dev.vida.vifada.MorphSource;
import dev.vida.vifada.VifadaEngineVersion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Дисковый кеш байткода после {@link dev.vida.vifada.Transformer}: ключ —
 * SHA-256 от входных байт класса, содержимого морфов и метаданных движка.
 *
 * <p>Опционально держит bounded in-memory LRU поверх диска ({@code -Dvida.transformCache.memEntries}).
 */
@ApiStatus.Internal
public final class TransformBytecodeCache {

    private static final int MEM_MAX =
            Integer.parseInt(System.getProperty("vida.transformCache.memEntries", "512"));

    private final Path directory;
    private final Map<String, byte[]> memoria =
            java.util.Collections.synchronizedMap(
                    new LinkedHashMap<>(256, 0.75f, true) {
                        @Override
                        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                            return size() > MEM_MAX;
                        }
                    });

    public TransformBytecodeCache(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory");
    }

    public Path directory() {
        return directory;
    }

    /** Строит детерминированный ключ кеша. */
    public static String computeKey(
            String internalClassName,
            byte[] classfileBuffer,
            List<MorphSource> morphs,
            String mappingFingerprint) {
        Objects.requireNonNull(internalClassName, "internalClassName");
        Objects.requireNonNull(classfileBuffer, "classfileBuffer");
        Objects.requireNonNull(morphs, "morphs");
        String fp = mappingFingerprint == null ? "nomap" : mappingFingerprint;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(internalClassName.getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0);
            md.update(classfileBuffer);
            md.update(VifadaEngineVersion.VERSION.getBytes(StandardCharsets.UTF_8));
            md.update((byte) 0);
            md.update(fp.getBytes(StandardCharsets.UTF_8));
            for (MorphSource src : morphs) {
                md.update((byte) 1);
                md.update(src.internalName().getBytes(StandardCharsets.UTF_8));
                md.update(src.bytes());
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public byte[] get(String hexKey) {
        byte[] mem = memoria.get(hexKey);
        if (mem != null) {
            return mem.clone();
        }
        Path f = directory.resolve(hexKey + ".vtc");
        try {
            if (!Files.isRegularFile(f)) {
                return null;
            }
            byte[] disk = Files.readAllBytes(f);
            memoria.put(hexKey, disk);
            return disk;
        } catch (IOException e) {
            return null;
        }
    }

    public void put(String hexKey, byte[] bytes) {
        memoria.put(hexKey, bytes.clone());
        try {
            Files.createDirectories(directory);
            Path f = directory.resolve(hexKey + ".vtc");
            Files.write(
                    f,
                    bytes,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.WRITE);
        } catch (IOException ignored) {
            // cache is best-effort
        }
    }
}
