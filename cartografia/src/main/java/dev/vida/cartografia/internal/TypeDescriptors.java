/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.cartografia.internal;

import dev.vida.core.ApiStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Конвертация Java source-типов в JVM descriptors.
 *
 * <p>Parser умеет: примитивы ({@code int, void, long, boolean, ...}), массивы
 * ({@code int[]}, {@code java.lang.String[][]}), произвольные классы
 * ({@code com.example.Foo.Bar}). Вложенные классы в Proguard-мэппингах уже
 * представлены с {@code $}, поэтому дополнительная обработка не требуется.
 *
 * <p>Модуль внутренний — используется читателями мэппингов.
 */
@ApiStatus.Internal
public final class TypeDescriptors {

    private TypeDescriptors() {}

    /** Преобразует Java source-type ({@code "int[]"}, {@code "java.lang.String"}) в JVM descriptor. */
    public static String sourceToDescriptor(String sourceType) {
        Objects.requireNonNull(sourceType, "sourceType");
        String t = sourceType.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException("empty source type");
        }
        int arrayDims = 0;
        while (t.endsWith("[]")) {
            arrayDims++;
            t = t.substring(0, t.length() - 2).trim();
        }
        StringBuilder sb = new StringBuilder(t.length() + arrayDims + 2);
        for (int i = 0; i < arrayDims; i++) {
            sb.append('[');
        }
        appendPrimitiveOrClass(sb, t);
        return sb.toString();
    }

    /**
     * Собирает JVM-дескриптор метода из Java source-типов:
     * {@code (paramDesc*)returnDesc}.
     *
     * @param paramSourceTypes список исходных типов параметров (может быть пустым)
     * @param returnSourceType тип возврата ({@code "void"}, {@code "int"}, {@code "java.lang.String"}…)
     */
    public static String methodDescriptor(List<String> paramSourceTypes, String returnSourceType) {
        Objects.requireNonNull(paramSourceTypes, "paramSourceTypes");
        Objects.requireNonNull(returnSourceType, "returnSourceType");
        StringBuilder sb = new StringBuilder(32);
        sb.append('(');
        for (String p : paramSourceTypes) {
            sb.append(sourceToDescriptor(p));
        }
        sb.append(')');
        sb.append(sourceToDescriptor(returnSourceType));
        return sb.toString();
    }

    /**
     * Разбирает список параметров вида {@code "int,java.lang.String,float[]"}
     * (без окружающих скобок) на массив source-типов.
     *
     * <p>Пустая строка → пустой список.
     */
    public static List<String> splitParams(String paramList) {
        Objects.requireNonNull(paramList, "paramList");
        String t = paramList.trim();
        if (t.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>(4);
        int depth = 0;
        int start = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '<') depth++;
            else if (c == '>') depth--;
            else if (c == ',' && depth == 0) {
                out.add(t.substring(start, i).trim());
                start = i + 1;
            }
        }
        out.add(t.substring(start).trim());
        return out;
    }

    private static void appendPrimitiveOrClass(StringBuilder sb, String t) {
        switch (t) {
            case "void"    -> sb.append('V');
            case "boolean" -> sb.append('Z');
            case "byte"    -> sb.append('B');
            case "char"    -> sb.append('C');
            case "short"   -> sb.append('S');
            case "int"     -> sb.append('I');
            case "long"    -> sb.append('J');
            case "float"   -> sb.append('F');
            case "double"  -> sb.append('D');
            default -> {
                sb.append('L');
                for (int i = 0; i < t.length(); i++) {
                    char c = t.charAt(i);
                    sb.append(c == '.' ? '/' : c);
                }
                sb.append(';');
            }
        }
    }
}
