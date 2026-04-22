/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.core;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Версия по спецификации <a href="https://semver.org/spec/v2.0.0.html">SemVer 2.0.0</a>.
 *
 * <p>Формат: {@code MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]}.
 *
 * <h2>Правила сравнения (из SemVer §11)</h2>
 * <ol>
 *   <li>Порядок определяется парой {@code (MAJOR, MINOR, PATCH)} как троек чисел.</li>
 *   <li>Предрелизная версия {@code 1.0.0-alpha} меньше {@code 1.0.0}.</li>
 *   <li>Идентификаторы предрелиза сравниваются как <i>числа</i>, если оба числовые;
 *       иначе лексикографически; числовые идентификаторы меньше нечисловых;
 *       более короткий набор идентификаторов меньше длинного с тем же префиксом.</li>
 *   <li>Метаданные билда ({@code +…}) в сравнении <b>игнорируются</b>.</li>
 * </ol>
 *
 * <p>Тип иммутабелен и thread-safe.
 */
@ApiStatus.Stable
public final class Version implements Comparable<Version> {

    private final int major;
    private final int minor;
    private final int patch;
    /** Список идентификаторов предрелиза; пустой список ⇒ релизная версия. */
    private final List<String> preRelease;
    /** Список идентификаторов build-метаданных; не участвует в сравнении. */
    private final List<String> buildMeta;

    private Version(int major, int minor, int patch, List<String> preRelease, List<String> buildMeta) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = List.copyOf(preRelease);
        this.buildMeta = List.copyOf(buildMeta);
    }

    // ------------------------------------------------------------ factories

    public static Version of(int major, int minor, int patch) {
        return new Version(requireNonNegative(major, "major"),
                requireNonNegative(minor, "minor"),
                requireNonNegative(patch, "patch"),
                List.of(), List.of());
    }

    /** Строгий парсер; бросает {@link IllegalArgumentException} на невалидном входе. */
    public static Version parse(String raw) {
        Objects.requireNonNull(raw, "raw");
        if (raw.isEmpty()) {
            throw new IllegalArgumentException("empty version string");
        }

        String core = raw;
        String pre = null;
        String build = null;

        int buildIdx = core.indexOf('+');
        if (buildIdx >= 0) {
            build = core.substring(buildIdx + 1);
            core = core.substring(0, buildIdx);
        }
        int preIdx = core.indexOf('-');
        if (preIdx >= 0) {
            pre = core.substring(preIdx + 1);
            core = core.substring(0, preIdx);
        }
        if (pre != null && pre.isEmpty()) {
            throw new IllegalArgumentException("empty pre-release section in version: " + raw);
        }
        if (build != null && build.isEmpty()) {
            throw new IllegalArgumentException("empty build metadata section in version: " + raw);
        }

        String[] coreParts = core.split("\\.", -1);
        if (coreParts.length != 3) {
            throw new IllegalArgumentException("version core must be MAJOR.MINOR.PATCH, got: " + raw);
        }
        int major = parseNumeric(coreParts[0], "major", raw);
        int minor = parseNumeric(coreParts[1], "minor", raw);
        int patch = parseNumeric(coreParts[2], "patch", raw);

        List<String> preIds = pre == null ? List.of() : splitAndValidateIds(pre, true, raw);
        List<String> buildIds = build == null ? List.of() : splitAndValidateIds(build, false, raw);

        return new Version(major, minor, patch, preIds, buildIds);
    }

    /** Нестрогий парсер: при ошибке возвращает {@code empty}. */
    public static Optional<Version> tryParse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(parse(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static boolean isValid(String raw) {
        return tryParse(raw).isPresent();
    }

    // -------------------------------------------------------------- accessors

    public int major() { return major; }
    public int minor() { return minor; }
    public int patch() { return patch; }
    public List<String> preRelease() { return preRelease; }
    public List<String> buildMeta() { return buildMeta; }

    /** {@code true}, если есть хотя бы один pre-release-идентификатор. */
    public boolean isPreRelease() { return !preRelease.isEmpty(); }

    /** Возвращает ту же версию, но без pre-release и build-метаданных. */
    public Version stableCore() {
        if (!isPreRelease() && buildMeta.isEmpty()) return this;
        return new Version(major, minor, patch, List.of(), List.of());
    }

    /** Бампер MAJOR: (M,m,p) → (M+1, 0, 0). */
    public Version bumpMajor() { return Version.of(Math.addExact(major, 1), 0, 0); }

    /** Бампер MINOR: (M,m,p) → (M, m+1, 0). */
    public Version bumpMinor() { return Version.of(major, Math.addExact(minor, 1), 0); }

    /** Бампер PATCH: (M,m,p) → (M, m, p+1). */
    public Version bumpPatch() { return Version.of(major, minor, Math.addExact(patch, 1)); }

    // -------------------------------------------------------------- compare

    @Override
    public int compareTo(Version other) {
        int c = Integer.compare(this.major, other.major);
        if (c != 0) return c;
        c = Integer.compare(this.minor, other.minor);
        if (c != 0) return c;
        c = Integer.compare(this.patch, other.patch);
        if (c != 0) return c;
        return comparePreRelease(this.preRelease, other.preRelease);
    }

    /** SemVer §11.4: pre-release-идентификаторы. */
    private static int comparePreRelease(List<String> a, List<String> b) {
        // Релиз > предрелиза того же ядра.
        if (a.isEmpty() && b.isEmpty()) return 0;
        if (a.isEmpty()) return 1;
        if (b.isEmpty()) return -1;

        int n = Math.min(a.size(), b.size());
        for (int i = 0; i < n; i++) {
            int c = comparePreIdentifier(a.get(i), b.get(i));
            if (c != 0) return c;
        }
        return Integer.compare(a.size(), b.size());
    }

    private static int comparePreIdentifier(String a, String b) {
        boolean aNum = isAllDigits(a);
        boolean bNum = isAllDigits(b);
        if (aNum && bNum) {
            // Сравниваем как числа, используя BigInteger? На практике достаточно long,
            // но технически идентификатор может быть длиннее; используем строковое
            // сравнение с выравниванием по длине.
            return compareNumericString(a, b);
        }
        if (aNum) return -1;
        if (bNum) return 1;
        return a.compareTo(b);
    }

    private static int compareNumericString(String a, String b) {
        int c = Integer.compare(a.length(), b.length());
        return c != 0 ? c : a.compareTo(b);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Version other)) return false;
        return major == other.major
                && minor == other.minor
                && patch == other.patch
                && preRelease.equals(other.preRelease);
        // buildMeta намеренно не участвует — per SemVer §10.
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, preRelease);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16);
        sb.append(major).append('.').append(minor).append('.').append(patch);
        if (!preRelease.isEmpty()) {
            sb.append('-');
            joinWithDots(sb, preRelease);
        }
        if (!buildMeta.isEmpty()) {
            sb.append('+');
            joinWithDots(sb, buildMeta);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------- internals

    private static int requireNonNegative(int v, String name) {
        if (v < 0) {
            throw new IllegalArgumentException(name + " must be >= 0, got " + v);
        }
        return v;
    }

    private static int parseNumeric(String s, String name, String raw) {
        if (s.isEmpty()) {
            throw new IllegalArgumentException(name + " is empty in version: " + raw);
        }
        if (s.length() > 1 && s.charAt(0) == '0') {
            throw new IllegalArgumentException(name + " has leading zero in version: " + raw);
        }
        for (int i = 0, n = s.length(); i < n; i++) {
            if (!isDigit(s.charAt(i))) {
                throw new IllegalArgumentException(name + " is not numeric in version: " + raw);
            }
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(name + " overflow in version: " + raw, ex);
        }
    }

    private static List<String> splitAndValidateIds(String group, boolean prerelease, String raw) {
        String[] ids = group.split("\\.", -1);
        for (String id : ids) {
            if (id.isEmpty()) {
                throw new IllegalArgumentException(
                        (prerelease ? "pre-release" : "build") + " contains empty identifier: " + raw);
            }
            if (prerelease && isAllDigits(id) && id.length() > 1 && id.charAt(0) == '0') {
                throw new IllegalArgumentException(
                        "numeric pre-release identifier has leading zero: " + raw);
            }
            for (int i = 0, n = id.length(); i < n; i++) {
                char c = id.charAt(i);
                if (!isAlphanumeric(c) && c != '-') {
                    throw new IllegalArgumentException(
                            "invalid character '" + c + "' in identifier '" + id + "' of version: " + raw);
                }
            }
        }
        return List.of(ids);
    }

    private static void joinWithDots(StringBuilder sb, List<String> parts) {
        for (int i = 0, n = parts.size(); i < n; i++) {
            if (i > 0) sb.append('.');
            sb.append(parts.get(i));
        }
    }

    private static boolean isDigit(char c) { return c >= '0' && c <= '9'; }

    private static boolean isAllDigits(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0, n = s.length(); i < n; i++) {
            if (!isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    private static boolean isAlphanumeric(char c) {
        return isDigit(c) || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
}
