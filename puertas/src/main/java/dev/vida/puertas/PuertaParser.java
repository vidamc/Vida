/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.puertas;

import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Парсер .ptr-файла.
 *
 * <p>Возвращает {@link ParseResult}, содержащий распарсенный файл плюс
 * список ошибок. Ошибки «мягкие» — парсер старается дочитать файл до конца,
 * чтобы выдать все проблемы сразу, но сам результат всё равно валиден
 * только если {@code errores.isEmpty()}.
 */
@ApiStatus.Preview("puertas")
public final class PuertaParser {

    private static final int VERSION_MAX_SOPORTADA = 1;

    private PuertaParser() {}

    /** Результат парсинга: либо успех, либо список ошибок. */
    public record ParseResult(PuertaArchivo archivo, List<PuertaError> errores) {
        public ParseResult {
            errores = List.copyOf(Objects.requireNonNull(errores, "errores"));
        }

        public boolean esExitoso() { return errores.isEmpty(); }
    }

    /** Парсит файл из строки (для тестов). */
    public static ParseResult parsear(String origen, String contenido) {
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(contenido, "contenido");
        try (BufferedReader rd = new BufferedReader(new StringReader(contenido))) {
            return parsear(origen, rd);
        } catch (IOException ex) {
            // чтение из StringReader не может бросить — маркер
            throw new AssertionError("unreachable", ex);
        }
    }

    /** Парсит файл с диска. */
    public static ParseResult parsear(Path archivo) throws IOException {
        Objects.requireNonNull(archivo, "archivo");
        try (BufferedReader rd = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
            return parsear(archivo.toString(), rd);
        }
    }

    /** Парсит произвольный Reader. */
    public static ParseResult parsear(String origen, Reader r) throws IOException {
        Objects.requireNonNull(origen, "origen");
        Objects.requireNonNull(r, "r");
        BufferedReader br = (r instanceof BufferedReader b) ? b : new BufferedReader(r);

        List<PuertaError> errores = new ArrayList<>();
        List<PuertaDirectiva> directivas = new ArrayList<>();
        String linea;
        int numLinea = 0;
        int version = -1;
        Namespace ns = null;

        while ((linea = br.readLine()) != null) {
            numLinea++;
            String limpia = quitarComentario(linea).strip();
            if (limpia.isEmpty()) continue;

            if (version == -1) {
                // Первая значимая строка — заголовок.
                Result<HeaderTokens, PuertaError> hr = parsearCabecera(origen, limpia);
                if (hr.isErr()) {
                    errores.add(hr.unwrapErr());
                    // Без валидного заголовка дальше невозможно.
                    return new ParseResult(
                            new PuertaArchivo(origen, 1, Namespace.INTERMEDIO, List.of()),
                            errores);
                }
                HeaderTokens h = hr.unwrap();
                if (h.version > VERSION_MAX_SOPORTADA) {
                    errores.add(new PuertaError.VersionNoSoportada(origen, h.version));
                    return new ParseResult(
                            new PuertaArchivo(origen, h.version, h.namespace, List.of()),
                            errores);
                }
                version = h.version;
                ns = h.namespace;
                continue;
            }

            Result<PuertaDirectiva, PuertaError> dr = parsearDirectiva(origen, numLinea, limpia);
            if (dr.isErr()) {
                errores.add(dr.unwrapErr());
            } else {
                directivas.add(dr.unwrap());
            }
        }

        if (version == -1) {
            errores.add(new PuertaError.CabeceraInvalida(origen, "", "файл пустой или без заголовка"));
            return new ParseResult(
                    new PuertaArchivo(origen, 1, Namespace.INTERMEDIO, List.of()),
                    errores);
        }

        return new ParseResult(new PuertaArchivo(origen, version, ns, directivas), errores);
    }

    // ----------------------------------------------------- header

    private record HeaderTokens(int version, Namespace namespace) {}

    private static Result<HeaderTokens, PuertaError> parsearCabecera(String origen, String linea) {
        // vida-puertas <ver> namespace=<ns>
        String[] partes = linea.split("\\s+");
        if (partes.length < 3 || !partes[0].equals("vida-puertas")) {
            return Result.err(new PuertaError.CabeceraInvalida(origen, linea,
                    "ожидалось 'vida-puertas <версия> namespace=<ns>'"));
        }
        int v;
        try {
            v = Integer.parseInt(partes[1]);
        } catch (NumberFormatException nfe) {
            return Result.err(new PuertaError.CabeceraInvalida(origen, linea,
                    "не числовая версия: " + partes[1]));
        }
        if (v < 1) {
            return Result.err(new PuertaError.CabeceraInvalida(origen, linea,
                    "версия < 1: " + v));
        }
        String ns = null;
        for (int i = 2; i < partes.length; i++) {
            if (partes[i].startsWith("namespace=")) {
                ns = partes[i].substring("namespace=".length());
            } else {
                return Result.err(new PuertaError.CabeceraInvalida(origen, linea,
                        "неизвестный параметр: " + partes[i]));
            }
        }
        if (ns == null) {
            return Result.err(new PuertaError.CabeceraInvalida(origen, linea,
                    "отсутствует namespace=<...>"));
        }
        Namespace n = Namespace.deClave(ns);
        if (n == null) {
            return Result.err(new PuertaError.NamespaceDesconocido(origen, ns));
        }
        return Result.ok(new HeaderTokens(v, n));
    }

    // ----------------------------------------------------- directive

    private static Result<PuertaDirectiva, PuertaError> parsearDirectiva(
            String origen, int numLinea, String linea) {
        String[] partes = linea.split("\\s+");
        if (partes.length < 3) {
            return Result.err(new PuertaError.DirectivaTruncada(origen, numLinea, linea));
        }

        Accion accion = switch (partes[0]) {
            case "accesible"  -> Accion.ACCESIBLE;
            case "extensible" -> Accion.EXTENSIBLE;
            case "mutable"    -> Accion.MUTABLE;
            default -> null;
        };
        if (accion == null) {
            return Result.err(new PuertaError.DirectivaInvalida(origen, numLinea, linea,
                    "неизвестное действие: " + partes[0]));
        }

        Objetivo objetivo = switch (partes[1]) {
            case "class"  -> Objetivo.CLASE;
            case "method" -> Objetivo.METODO;
            case "field"  -> Objetivo.CAMPO;
            default -> null;
        };
        if (objetivo == null) {
            return Result.err(new PuertaError.DirectivaInvalida(origen, numLinea, linea,
                    "неизвестная цель: " + partes[1]));
        }

        // class <internal>
        if (objetivo == Objetivo.CLASE) {
            if (partes.length != 3) {
                return Result.err(new PuertaError.DirectivaInvalida(origen, numLinea, linea,
                        "для class ожидается 3 токена, получено " + partes.length));
            }
            if (accion == Accion.MUTABLE) {
                return Result.err(new PuertaError.MutableNoAplicable(origen, numLinea, linea));
            }
            String cls = normalizarClase(partes[2]);
            return Result.ok(new PuertaDirectiva(
                    accion, objetivo, cls, Optional.empty(), Optional.empty(), numLinea));
        }

        // method <internal> <name> <desc>
        // field  <internal> <name> <desc>
        if (partes.length < 5) {
            return Result.err(new PuertaError.DirectivaTruncada(origen, numLinea, linea));
        }
        if (partes.length > 5) {
            return Result.err(new PuertaError.DirectivaInvalida(origen, numLinea, linea,
                    "ожидается 5 токенов, получено " + partes.length));
        }
        if (objetivo == Objetivo.METODO && accion == Accion.MUTABLE) {
            return Result.err(new PuertaError.MutableNoAplicable(origen, numLinea, partes[0] + " method"));
        }

        String cls  = normalizarClase(partes[2]);
        String name = partes[3];
        String desc = partes[4];
        if (!descriptorValido(objetivo, desc)) {
            return Result.err(new PuertaError.DescriptorMalformado(origen, numLinea, desc));
        }
        return Result.ok(new PuertaDirectiva(
                accion, objetivo, cls, Optional.of(name), Optional.of(desc), numLinea));
    }

    // ----------------------------------------------------- helpers

    private static String quitarComentario(String s) {
        int i = s.indexOf('#');
        return i < 0 ? s : s.substring(0, i);
    }

    private static String normalizarClase(String s) {
        return s.replace('.', '/');
    }

    /** Минимальная проверка JVM-дескрипторов. */
    private static boolean descriptorValido(Objetivo o, String desc) {
        if (desc == null || desc.isEmpty()) return false;
        if (o == Objetivo.CAMPO) {
            return descriptorTipoValido(desc);
        }
        // method: (args)ret
        if (desc.charAt(0) != '(') return false;
        int cierre = desc.indexOf(')');
        if (cierre < 0) return false;
        int i = 1;
        while (i < cierre) {
            int consumido = consumirTipo(desc, i, cierre);
            if (consumido <= 0) return false;
            i += consumido;
        }
        String ret = desc.substring(cierre + 1);
        if (ret.equals("V")) return true;
        return descriptorTipoValido(ret);
    }

    private static boolean descriptorTipoValido(String desc) {
        int n = consumirTipo(desc, 0, desc.length());
        return n == desc.length();
    }

    /** Возвращает длину одного типа в descriptor; {@code 0} если невалиден. */
    private static int consumirTipo(String s, int start, int end) {
        if (start >= end) return 0;
        char c = s.charAt(start);
        return switch (c) {
            case 'B', 'C', 'D', 'F', 'I', 'J', 'S', 'Z' -> 1;
            case 'L' -> {
                int sc = s.indexOf(';', start + 1);
                if (sc < 0 || sc >= end || sc == start + 1) yield 0;
                yield (sc - start) + 1;
            }
            case '[' -> {
                int k = consumirTipo(s, start + 1, end);
                yield k == 0 ? 0 : k + 1;
            }
            default -> 0;
        };
    }
}
