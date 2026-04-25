/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia.io;

import dev.vida.cartografia.MappingError;
import dev.vida.cartografia.MappingTree;
import dev.vida.cartografia.Namespace;
import dev.vida.cartografia.internal.TypeDescriptors;
import dev.vida.core.ApiStatus;
import dev.vida.core.Result;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Читатель Mojang-овского proguard-формата мэппингов.
 *
 * <p>Вход — UTF-8 текст вида:
 * <pre>
 * # Comment
 * com.mojang.blaze3d.Blaze3D -&gt; dxe:
 *     int FRAME_START -&gt; a
 *     1:2:void setClearColor(float,float,float,float) -&gt; a
 *     void process(java.util.function.BooleanSupplier) -&gt; b
 * </pre>
 *
 * <p>Формат — named → obf: слева именованное Mojang-имя, справа обфусцированное
 * имя из runtime-jar. Cartografía хранит source namespace = обфусцированный,
 * поэтому ридер разворачивает порядок: при возврате
 * {@code source = obfNs, targets = [namedNs]}.
 *
 * <p>Разбор двухпроходный: сначала собираются имена классов (чтобы вычислить
 * обратное соответствие для дескрипторов), затем строится {@link MappingTree}
 * с JVM-дескрипторами в source namespace.
 */
@ApiStatus.Stable
public final class ProguardReader {

    private ProguardReader() {}

    /** Читает содержимое строки. Удобно для тестов. */
    public static Result<MappingTree, MappingError> readString(
            String sourceName, String content, Namespace obfNs, Namespace namedNs) {
        Objects.requireNonNull(content, "content");
        return read(sourceName, new StringReader(content), obfNs, namedNs);
    }

    /**
     * Читает поток и возвращает {@link MappingTree}.
     *
     * <p>{@code Reader} закрывается читателем самостоятельно.
     *
     * @param sourceName имя источника для сообщений об ошибках (например,
     *                   {@code "mojang_1_21_1.txt"})
     * @param in поток с proguard-мэппингами
     * @param obfNs namespace для обфусцированных имён (source дерева)
     * @param namedNs namespace для читаемых Mojang-имён
     */
    /**
     * Как {@link #read}, плюс список внутренних имён классов из дескрипторов,
     * не сопоставившихся ни с одним классом в первом проходе.
     */
    public static Result<ProguardImportDiagnostics, MappingError> readWithDiagnostics(
            String sourceName, Reader in, Namespace obfNs, Namespace namedNs) {
        return readImpl(sourceName, in, obfNs, namedNs, true);
    }

    public static Result<MappingTree, MappingError> read(
            String sourceName, Reader in, Namespace obfNs, Namespace namedNs) {
        Result<ProguardImportDiagnostics, MappingError> r =
                readImpl(sourceName, in, obfNs, namedNs, false);
        if (r.isErr()) {
            return Result.err(r.unwrapErr());
        }
        return Result.ok(r.unwrap().tree());
    }

    private static Result<ProguardImportDiagnostics, MappingError> readImpl(
            String sourceName,
            Reader in,
            Namespace obfNs,
            Namespace namedNs,
            boolean collectDiagnostics) {
        Objects.requireNonNull(sourceName, "sourceName");
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(obfNs, "obfNs");
        Objects.requireNonNull(namedNs, "namedNs");
        if (obfNs.equals(namedNs)) {
            throw new IllegalArgumentException("obfNs and namedNs must differ");
        }

        List<RawClass> rawClasses = new ArrayList<>(1024);
        Map<String, String> namedToObf = new HashMap<>(2048);

        try (BufferedReader br = new BufferedReader(in)) {
            String line;
            int lineNum = 0;
            RawClass current = null;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (line.isEmpty()) continue;
                String trimmed = stripTrailing(line);
                if (trimmed.isEmpty()) continue;

                char first = trimmed.charAt(0);
                if (first == '#') continue;

                boolean indented = isWhitespace(line.charAt(0));
                if (!indented) {
                    MappingError err = parseClassHeader(sourceName, lineNum, trimmed, rawClasses, namedToObf);
                    if (err != null) return Result.err(err);
                    current = rawClasses.get(rawClasses.size() - 1);
                } else {
                    if (current == null) {
                        return Result.err(new MappingError.SyntaxError(
                                sourceName, lineNum,
                                "member line without enclosing class"));
                    }
                    MappingError err = parseMember(sourceName, lineNum, trimmed.trim(), current);
                    if (err != null) return Result.err(err);
                }
            }
        } catch (IOException e) {
            return Result.err(new MappingError.IoError(sourceName, e.getMessage()));
        }

        Set<String> unresolved = collectDiagnostics ? new LinkedHashSet<>(64) : null;
        Result<MappingTree, MappingError> tree = buildTree(rawClasses, namedToObf, obfNs, namedNs, unresolved);
        if (tree.isErr()) {
            return Result.err(tree.unwrapErr());
        }
        List<String> report = unresolved == null
                ? List.of()
                : List.copyOf(unresolved);
        return Result.ok(new ProguardImportDiagnostics(tree.unwrap(), report));
    }

    // =============================================================== stage 1

    private static MappingError parseClassHeader(
            String src, int line, String text,
            List<RawClass> out, Map<String, String> namedToObf) {
        int arrow = text.indexOf(" -> ");
        if (arrow < 0) {
            return new MappingError.SyntaxError(src, line, "expected ' -> ' in class header");
        }
        if (!text.endsWith(":")) {
            return new MappingError.SyntaxError(src, line, "expected ':' at end of class header");
        }
        String named = text.substring(0, arrow).trim();
        String obf = text.substring(arrow + 4, text.length() - 1).trim();
        if (named.isEmpty() || obf.isEmpty()) {
            return new MappingError.SyntaxError(src, line, "empty class name");
        }
        String namedInternal = named.replace('.', '/');
        String obfInternal = obf.replace('.', '/');
        out.add(new RawClass(obfInternal, namedInternal));
        namedToObf.put(namedInternal, obfInternal);
        return null;
    }

    private static MappingError parseMember(String src, int line, String text, RawClass cls) {
        int arrow = text.indexOf(" -> ");
        if (arrow < 0) {
            return new MappingError.SyntaxError(src, line, "expected ' -> ' in member");
        }
        String left = text.substring(0, arrow).trim();
        String right = text.substring(arrow + 4).trim();
        if (right.isEmpty()) {
            return new MappingError.SyntaxError(src, line, "missing obfuscated member name");
        }

        if (left.indexOf('(') >= 0) {
            return parseMethod(src, line, left, right, cls);
        }
        return parseField(src, line, left, right, cls);
    }

    private static MappingError parseField(
            String src, int line, String left, String right, RawClass cls) {
        int lastSpace = left.lastIndexOf(' ');
        if (lastSpace < 0) {
            return new MappingError.SyntaxError(src, line, "field must be '<type> <name>'");
        }
        String type = left.substring(0, lastSpace).trim();
        String name = left.substring(lastSpace + 1).trim();
        if (type.isEmpty() || name.isEmpty()) {
            return new MappingError.SyntaxError(src, line, "empty field type or name");
        }
        String desc;
        try {
            desc = TypeDescriptors.sourceToDescriptor(type);
        } catch (IllegalArgumentException iae) {
            return new MappingError.SyntaxError(src, line, "bad field type: " + iae.getMessage());
        }
        cls.fields.add(new RawMember(name, desc, right));
        return null;
    }

    private static MappingError parseMethod(
            String src, int line, String left, String right, RawClass cls) {
        int afterLineInfo = stripLineInfo(left);
        String spec = left.substring(afterLineInfo).trim();
        int openParen = spec.indexOf('(');
        int closeParen = spec.lastIndexOf(')');
        if (openParen < 0 || closeParen < 0 || closeParen <= openParen) {
            return new MappingError.SyntaxError(src, line, "bad method signature");
        }
        String beforeParen = spec.substring(0, openParen).trim();
        String paramList = spec.substring(openParen + 1, closeParen);
        int lastSpace = beforeParen.lastIndexOf(' ');
        if (lastSpace < 0) {
            return new MappingError.SyntaxError(src, line, "method must be '<returnType> <name>(<params>)'");
        }
        String retType = beforeParen.substring(0, lastSpace).trim();
        String name = beforeParen.substring(lastSpace + 1).trim();
        if (retType.isEmpty() || name.isEmpty()) {
            return new MappingError.SyntaxError(src, line, "empty method return type or name");
        }

        String desc;
        try {
            List<String> params = TypeDescriptors.splitParams(paramList);
            desc = TypeDescriptors.methodDescriptor(params, retType);
        } catch (IllegalArgumentException iae) {
            return new MappingError.SyntaxError(src, line, "bad method signature: " + iae.getMessage());
        }
        cls.methods.add(new RawMember(name, desc, right));
        return null;
    }

    /**
     * Пропускает опциональный префикс {@code "<int>:<int>:"} у методов
     * (номера строк исходника). Возвращает индекс после префикса или 0, если
     * префикса нет.
     */
    private static int stripLineInfo(String s) {
        int i = 0, n = s.length();
        while (i < n && isDigit(s.charAt(i))) i++;
        if (i == 0 || i >= n || s.charAt(i) != ':') return 0;
        int colonA = i;
        i++;
        int j = i;
        while (j < n && isDigit(s.charAt(j))) j++;
        if (j == i || j >= n || s.charAt(j) != ':') return 0;
        return j + 1;
    }

    // =============================================================== stage 2

    private static Result<MappingTree, MappingError> buildTree(
            List<RawClass> raw,
            Map<String, String> namedToObf,
            Namespace obfNs,
            Namespace namedNs,
            Set<String> unresolvedCollector) {

        MappingTree.Builder b = MappingTree.builder(obfNs, namedNs);
        for (RawClass rc : raw) {
            MappingTree.ClassBuilder cb;
            try {
                cb = b.addClass(rc.obfInternal, rc.namedInternal);
            } catch (IllegalStateException dup) {
                return Result.err(new MappingError.DuplicateEntry(rc.obfInternal, obfNs.name()));
            }
            for (RawMember f : rc.fields) {
                String obfDesc = remapDescriptor(f.namedDesc, namedToObf, unresolvedCollector);
                cb.addField(obfDesc, f.obfName, f.namedName);
            }
            for (RawMember m : rc.methods) {
                String obfDesc = remapDescriptor(m.namedDesc, namedToObf, unresolvedCollector);
                cb.addMethod(obfDesc, m.obfName, m.namedName);
            }
            cb.done();
        }
        return Result.ok(b.build());
    }

    /** Замена namedClass → obfClass внутри JVM-дескриптора. */
    private static String remapDescriptor(
            String desc, Map<String, String> namedToObf, Set<String> unresolvedCollector) {
        StringBuilder out = new StringBuilder(desc.length() + 4);
        int i = 0, n = desc.length();
        while (i < n) {
            char c = desc.charAt(i);
            if (c == 'L') {
                int end = desc.indexOf(';', i + 1);
                if (end < 0) {
                    out.append(desc, i, n);
                    break;
                }
                String cls = desc.substring(i + 1, end);
                String obf = namedToObf.get(cls);
                if (obf == null) {
                    if (unresolvedCollector != null) {
                        unresolvedCollector.add(cls);
                    }
                    out.append('L').append(cls).append(';');
                } else {
                    out.append('L').append(obf).append(';');
                }
                i = end + 1;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    // =============================================================== helpers

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t';
    }

    private static String stripTrailing(String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) end--;
        return end == s.length() ? s : s.substring(0, end);
    }

    // =============================================================== data

    private static final class RawClass {
        final String obfInternal;
        final String namedInternal;
        final List<RawMember> fields = new ArrayList<>(4);
        final List<RawMember> methods = new ArrayList<>(8);

        RawClass(String obfInternal, String namedInternal) {
            this.obfInternal = obfInternal;
            this.namedInternal = namedInternal;
        }
    }

    private record RawMember(String namedName, String namedDesc, String obfName) {}
}
