/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.red;

import dev.vida.core.ApiStatus;
import dev.vida.core.Identifier;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Objects;

/**
 * Авто-кодек для Java record-пакетов.
 *
 * <p>Поддерживаемые типы компонентов:
 * {@code int}, {@code long}, {@code boolean}, {@code float}, {@code double},
 * {@link String}, {@link Identifier}, {@code enum}.
 */
@ApiStatus.Preview("red")
public final class CodificadorRegistros {

    private CodificadorRegistros() {}

    public static <T extends Record> CodecPaquete<T> para(Class<T> tipo) {
        Objects.requireNonNull(tipo, "tipo");
        if (!tipo.isRecord()) {
            throw new IllegalArgumentException("tipo no es record: " + tipo.getName());
        }
        RecordComponent[] componentes = tipo.getRecordComponents();
        Class<?>[] ctorArgs = new Class<?>[componentes.length];
        for (int i = 0; i < componentes.length; i++) {
            ctorArgs[i] = componentes[i].getType();
            validarTipoSoportado(componentes[i].getType(), tipo, componentes[i].getName());
        }

        final Constructor<T> ctor;
        try {
            ctor = tipo.getDeclaredConstructor(ctorArgs);
            ctor.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException("constructor canonico no encontrado para " + tipo.getName(), ex);
        }

        return new CodecPaquete<>() {
            @Override
            public byte[] codificar(T paquete) {
                Objects.requireNonNull(paquete, "paquete");
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (DataOutputStream out = new DataOutputStream(baos)) {
                        for (RecordComponent componente : componentes) {
                            Object valor = componente.getAccessor().invoke(paquete);
                            escribirComponente(out, componente.getType(), valor, componente.getName());
                        }
                    }
                    return baos.toByteArray();
                } catch (IllegalAccessException | InvocationTargetException | IOException ex) {
                    throw new IllegalArgumentException(
                            "no se pudo codificar " + tipo.getName() + ": " + ex.getMessage(), ex);
                }
            }

            @Override
            public T decodificar(byte[] payload) {
                Objects.requireNonNull(payload, "payload");
                Object[] args = new Object[componentes.length];
                try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
                    for (int i = 0; i < componentes.length; i++) {
                        RecordComponent componente = componentes[i];
                        args[i] = leerComponente(in, componente.getType(), componente.getName());
                    }
                    return ctor.newInstance(args);
                } catch (InstantiationException
                        | IllegalAccessException
                        | InvocationTargetException
                        | IOException ex) {
                    throw new IllegalArgumentException(
                            "no se pudo decodificar " + tipo.getName() + ": " + ex.getMessage(), ex);
                }
            }
        };
    }

    private static void validarTipoSoportado(Class<?> tipo, Class<?> owner, String nombre) {
        if (tipo == int.class
                || tipo == long.class
                || tipo == boolean.class
                || tipo == float.class
                || tipo == double.class
                || tipo == String.class
                || tipo == Identifier.class
                || tipo.isEnum()) {
            return;
        }
        throw new IllegalArgumentException(
                "tipo de campo no soportado en " + owner.getName() + "." + nombre + ": " + tipo.getName());
    }

    private static void escribirComponente(DataOutputStream out, Class<?> tipo, Object valor, String nombre)
            throws IOException {
        if (tipo == int.class) {
            out.writeInt((Integer) valor);
            return;
        }
        if (tipo == long.class) {
            out.writeLong((Long) valor);
            return;
        }
        if (tipo == boolean.class) {
            out.writeBoolean((Boolean) valor);
            return;
        }
        if (tipo == float.class) {
            out.writeFloat((Float) valor);
            return;
        }
        if (tipo == double.class) {
            out.writeDouble((Double) valor);
            return;
        }
        if (tipo == String.class) {
            out.writeUTF((String) validarNoNulo(valor, nombre));
            return;
        }
        if (tipo == Identifier.class) {
            out.writeUTF(((Identifier) validarNoNulo(valor, nombre)).toString());
            return;
        }
        if (tipo.isEnum()) {
            out.writeUTF(((Enum<?>) validarNoNulo(valor, nombre)).name());
            return;
        }
        throw new IllegalArgumentException("tipo no soportado: " + tipo.getName());
    }

    private static Object leerComponente(DataInputStream in, Class<?> tipo, String nombre) throws IOException {
        if (tipo == int.class) {
            return in.readInt();
        }
        if (tipo == long.class) {
            return in.readLong();
        }
        if (tipo == boolean.class) {
            return in.readBoolean();
        }
        if (tipo == float.class) {
            return in.readFloat();
        }
        if (tipo == double.class) {
            return in.readDouble();
        }
        if (tipo == String.class) {
            return in.readUTF();
        }
        if (tipo == Identifier.class) {
            return Identifier.parse(in.readUTF());
        }
        if (tipo.isEnum()) {
            String valor = in.readUTF();
            @SuppressWarnings({"rawtypes", "unchecked"})
            Enum<?> out = Enum.valueOf((Class<? extends Enum>) tipo.asSubclass(Enum.class), valor);
            return out;
        }
        throw new IllegalArgumentException("tipo no soportado al leer " + nombre + ": " + tipo.getName());
    }

    private static Object validarNoNulo(Object valor, String nombre) {
        if (valor == null) {
            throw new IllegalArgumentException("valor nulo en componente " + nombre);
        }
        return valor;
    }
}
