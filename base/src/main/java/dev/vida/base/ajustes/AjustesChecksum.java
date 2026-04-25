/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.ajustes;

import dev.vida.core.ApiStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Checksum estable del estado serializable de ajustes TOML para comparación en red.
 */
@ApiStatus.Stable
public final class AjustesChecksum {

    private AjustesChecksum() {}

    /** SHA-256 hex de los bytes UTF-8 del texto normalizado. */
    public static String sha256Hex(String contenidoTomlNormalizado) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    contenidoTomlNormalizado.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
