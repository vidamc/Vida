/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import dev.vida.core.ApiStatus;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Снимок строковых значений настроек с сервера на клиент (только пути, разрешённые
 * {@code dev.vida.base.ajustes.AjustesSincronizacionCatalogo}).
 */
@ApiStatus.Stable
public record PaqueteAjustesSincronizacionServidor(String modId, Map<String, String> valores)
        implements PaqueteServidor {

    public PaqueteAjustesSincronizacionServidor {
        modId = Objects.requireNonNull(modId, "modId");
        valores = Map.copyOf(valores);
    }

    /** Wire-codec версии {@code 1}. */
    public static CodecPaquete<PaqueteAjustesSincronizacionServidor> codec() {
        return new CodecPaquete<>() {
            @Override
            public byte[] codificar(PaqueteAjustesSincronizacionServidor p) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (DataOutputStream out = new DataOutputStream(baos)) {
                        out.writeUTF(p.modId());
                        out.writeInt(p.valores().size());
                        for (var e : p.valores().entrySet()) {
                            out.writeUTF(e.getKey());
                            out.writeUTF(e.getValue());
                        }
                    }
                    return baos.toByteArray();
                } catch (IOException ex) {
                    throw new IllegalArgumentException(ex);
                }
            }

            @Override
            public PaqueteAjustesSincronizacionServidor decodificar(byte[] payload) {
                try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
                    String mid = in.readUTF();
                    int n = in.readInt();
                    if (n < 0 || n > 10_000) {
                        throw new IOException("bad entry count");
                    }
                    Map<String, String> m = new LinkedHashMap<>();
                    for (int i = 0; i < n; i++) {
                        m.put(in.readUTF(), in.readUTF());
                    }
                    return new PaqueteAjustesSincronizacionServidor(mid, m);
                } catch (IOException ex) {
                    throw new IllegalArgumentException(ex);
                }
            }

            @Override
            public int maxCargaBytes() {
                return 4_000_000;
            }
        };
    }
}
