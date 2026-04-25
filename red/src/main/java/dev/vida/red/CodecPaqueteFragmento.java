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
import java.util.Objects;
import java.util.zip.CRC32;

@ApiStatus.Internal
final class CodecPaqueteFragmento {

    private CodecPaqueteFragmento() {}

    static byte[] codificarCliente(PaqueteClienteCargaFragmento p) throws IOException {
        return codificar(
                p.sesion(), p.indice(), p.total(), p.crc32(), p.longitudTotal(), p.fragmento());
    }

    static PaqueteClienteCargaFragmento decodificarCliente(byte[] payload) throws IOException {
        Object[] o = decodificar(payload);
        return new PaqueteClienteCargaFragmento(
                (Long) o[0], (Integer) o[1], (Integer) o[2], (Integer) o[3], (Integer) o[4], (byte[]) o[5]);
    }

    static byte[] codificarServidor(PaqueteServidorCargaFragmento p) throws IOException {
        return codificar(
                p.sesion(), p.indice(), p.total(), p.crc32(), p.longitudTotal(), p.fragmento());
    }

    static PaqueteServidorCargaFragmento decodificarServidor(byte[] payload) throws IOException {
        Object[] o = decodificar(payload);
        return new PaqueteServidorCargaFragmento(
                (Long) o[0], (Integer) o[1], (Integer) o[2], (Integer) o[3], (Integer) o[4], (byte[]) o[5]);
    }

    private static byte[] codificar(long sesion, int indice, int total, int crc32, int lenTotal, byte[] frag)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeLong(sesion);
            out.writeInt(indice);
            out.writeInt(total);
            out.writeInt(crc32);
            out.writeInt(lenTotal);
            out.writeInt(frag.length);
            out.write(frag);
        }
        return baos.toByteArray();
    }

    private static Object[] decodificar(byte[] payload) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            long sesion = in.readLong();
            int indice = in.readInt();
            int total = in.readInt();
            int crc32 = in.readInt();
            int lenTotal = in.readInt();
            int flen = in.readInt();
            if (flen < 0 || flen > 16_000_000) {
                throw new IOException("bad fragment length");
            }
            byte[] frag = new byte[flen];
            in.readFully(frag);
            return new Object[] {sesion, indice, total, crc32, lenTotal, frag};
        }
    }

    static int crc32(byte[] full) {
        CRC32 c = new CRC32();
        c.update(full);
        return (int) c.getValue();
    }

    static CodecPaquete<PaqueteClienteCargaFragmento> codecCliente() {
        return new CodecPaquete<>() {
            @Override
            public byte[] codificar(PaqueteClienteCargaFragmento paquete) {
                try {
                    return CodecPaqueteFragmento.codificarCliente(paquete);
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }

            @Override
            public PaqueteClienteCargaFragmento decodificar(byte[] payload) {
                try {
                    return CodecPaqueteFragmento.decodificarCliente(payload);
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }

            @Override
            public int maxCargaBytes() {
                return 8_000_000;
            }
        };
    }

    static CodecPaquete<PaqueteServidorCargaFragmento> codecServidor() {
        return new CodecPaquete<>() {
            @Override
            public byte[] codificar(PaqueteServidorCargaFragmento paquete) {
                try {
                    return CodecPaqueteFragmento.codificarServidor(paquete);
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }

            @Override
            public PaqueteServidorCargaFragmento decodificar(byte[] payload) {
                try {
                    return CodecPaqueteFragmento.decodificarServidor(payload);
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }

            @Override
            public int maxCargaBytes() {
                return 8_000_000;
            }
        };
    }
}
