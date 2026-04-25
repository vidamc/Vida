/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.base.catalogo.sync;

import dev.vida.base.catalogo.Catalogo;
import dev.vida.base.catalogo.CatalogoClave;
import dev.vida.core.ApiStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Стабильный digest снимка {@link Catalogo} для сравнения и Tejido-фазы 1.
 */
@ApiStatus.Preview("catalogo-sync")
public final class CatalogoDigestUtil {

    public static final int ESQUEMA_V1 = 1;

    private CatalogoDigestUtil() {}

    public static <T> CatalogoHuella huellaV1(Catalogo<T> catalogo) {
        Objects.requireNonNull(catalogo, "catalogo");
        StringBuilder b = new StringBuilder(256);
        b.append(ESQUEMA_V1).append('\n');
        b.append(catalogo.reestroId().toString()).append('\n');
        b.append(catalogo.claseValor().getName()).append('\n');
        List<CatalogoClave<T>> claves = new ArrayList<>(catalogo.claves());
        claves.sort(Comparator.comparing(c -> c.toString()));
        for (CatalogoClave<T> c : claves) {
            b.append(c.toString()).append('\n');
            T v = catalogo.obtener(c).orElse(null);
            b.append(v == null ? "-" : (v.getClass().getName() + '@' + Integer.toHexString(Objects.hashCode(v))))
                    .append('\n');
        }
        byte[] utf8 = b.toString().getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(utf8);
            return new CatalogoHuella(ESQUEMA_V1, HexFormat.of().formatHex(dig));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
