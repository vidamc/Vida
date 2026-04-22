/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Диапазон версий для декларации зависимостей в {@code vida.mod.json}.
 *
 * <h2>Поддерживаемый синтаксис (NPM-совместимый)</h2>
 * <pre>
 *   *                 — любая версия (включая pre-release)
 *   &lt;эмпты&gt;           — то же, что *
 *   =1.2.3            — точное равенство
 *   1.2.3             — точное равенство (= опционально)
 *   &gt;1.2.3, &gt;=1.2.3   — больше / больше-или-равно
 *   &lt;1.2.3, &lt;=1.2.3   — меньше / меньше-или-равно
 *   ^1.2.3            — совместимая: &gt;=1.2.3 &lt;2.0.0
 *                       если major=0, то ^0.2.3 = &gt;=0.2.3 &lt;0.3.0
 *                       если major=0 и minor=0, то ^0.0.3 = &gt;=0.0.3 &lt;0.0.4
 *   ~1.2.3            — приблизительно: &gt;=1.2.3 &lt;1.3.0
 *   ~1.2              — &gt;=1.2.0 &lt;1.3.0
 *   A &amp;&amp; B            — пересечение ограничений (также допустим пробел между сравнителями)
 *   A || B            — объединение
 * </pre>
 *
 * <p>Pre-release-версии по умолчанию <i>не</i> удовлетворяют числовым
 * ограничениям, если в самом диапазоне не упомянут их родительский
 * {@code MAJOR.MINOR.PATCH} с pre-release (NPM-правило). Это уменьшает число
 * сюрпризов для end-user’ов.
 */
@ApiStatus.Stable
public final class VersionRange {

    /** Диапазон, принимающий любую версию. Синглтон. */
    public static final VersionRange ANY = new VersionRange(List.of(List.of(Comparator.any())), "*");

    /** DNF: OR-группы, каждая из которых — AND-конъюнкция компараторов. */
    private final List<List<Comparator>> clauses;
    private final String raw;

    private VersionRange(List<List<Comparator>> clauses, String raw) {
        this.clauses = clauses;
        this.raw = raw;
    }

    // ------------------------------------------------------------ factories

    public static VersionRange parse(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || "*".equals(trimmed)) {
            return ANY;
        }

        String[] orParts = splitTopLevel(trimmed);
        List<List<Comparator>> out = new ArrayList<>(orParts.length);
        for (String or : orParts) {
            String s = or.trim();
            if (s.isEmpty() || "*".equals(s)) {
                out.add(List.of(Comparator.any()));
                continue;
            }
            out.add(parseConjunction(s, raw));
        }
        return new VersionRange(List.copyOf(out), raw);
    }

    public static Optional<VersionRange> tryParse(String raw) {
        if (raw == null) return Optional.empty();
        try {
            return Optional.of(parse(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static VersionRange exact(Version v) {
        return new VersionRange(List.of(List.of(Comparator.eq(v))), "=" + v);
    }

    // -------------------------------------------------------------- matching

    /** Проверяет, удовлетворяет ли версия диапазону. */
    public boolean matches(Version v) {
        Objects.requireNonNull(v, "v");
        for (List<Comparator> conj : clauses) {
            if (matchesAll(conj, v)) return true;
        }
        return false;
    }

    private static boolean matchesAll(List<Comparator> conj, Version v) {
        // NPM-правило: pre-release допускается только если в конъюнкции есть
        // компаратор с тем же (major,minor,patch) и тоже с pre-release.
        if (v.isPreRelease()) {
            boolean seeded = false;
            for (Comparator c : conj) {
                if (c.pivot != null && c.pivot.isPreRelease()
                        && c.pivot.major() == v.major()
                        && c.pivot.minor() == v.minor()
                        && c.pivot.patch() == v.patch()) {
                    seeded = true;
                    break;
                }
            }
            if (!seeded) return false;
        }
        for (Comparator c : conj) {
            if (!c.test(v)) return false;
        }
        return true;
    }

    /** Возвращает исходное строковое представление. */
    public String raw() { return raw; }

    @Override public String toString() { return raw; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VersionRange other)) return false;
        return clauses.equals(other.clauses);
    }

    @Override public int hashCode() { return clauses.hashCode(); }

    // ===================================================================
    //                            PARSING
    // ===================================================================

    private static String[] splitTopLevel(String s) {
        List<String> out = new ArrayList<>(2);
        int start = 0;
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == '|' && s.charAt(i + 1) == '|') {
                out.add(s.substring(start, i));
                start = i + 2;
                i++;
            }
        }
        out.add(s.substring(start));
        return out.toArray(new String[0]);
    }

    private static List<Comparator> parseConjunction(String s, String original) {
        List<Comparator> acc = new ArrayList<>(2);
        // Токены разделены пробелами или "&&"
        String normalized = s.replace("&&", " ").trim();
        String[] tokens = normalized.split("\\s+");
        for (String tok : tokens) {
            if (tok.isEmpty()) continue;
            parseToken(tok, acc, original);
        }
        if (acc.isEmpty()) {
            throw new IllegalArgumentException("empty version range: '" + original + "'");
        }
        return List.copyOf(acc);
    }

    private static void parseToken(String tok, List<Comparator> acc, String original) {
        char c0 = tok.charAt(0);
        String rest;
        switch (c0) {
            case '^' -> {
                rest = tok.substring(1).trim();
                Version v = parseVer(rest, original);
                Version upper = caretUpperBound(v);
                acc.add(Comparator.gte(v));
                acc.add(Comparator.lt(upper));
            }
            case '~' -> {
                rest = tok.substring(1).trim();
                Version v = parseVer(rest, original);
                Version upper = tildeUpperBound(v);
                acc.add(Comparator.gte(v));
                acc.add(Comparator.lt(upper));
            }
            case '>' -> {
                if (tok.length() > 1 && tok.charAt(1) == '=') {
                    acc.add(Comparator.gte(parseVer(tok.substring(2).trim(), original)));
                } else {
                    acc.add(Comparator.gt(parseVer(tok.substring(1).trim(), original)));
                }
            }
            case '<' -> {
                if (tok.length() > 1 && tok.charAt(1) == '=') {
                    acc.add(Comparator.lte(parseVer(tok.substring(2).trim(), original)));
                } else {
                    acc.add(Comparator.lt(parseVer(tok.substring(1).trim(), original)));
                }
            }
            case '=' -> addExactOrXRange(tok.substring(1).trim(), acc, original);
            default -> addExactOrXRange(tok, acc, original);
        }
    }

    /**
     * Обрабатывает X-ranges ({@code 1.x}, {@code 1.X}, {@code 1.*}, {@code 1.2.x})
     * или точное равенство.
     */
    private static void addExactOrXRange(String tok, List<Comparator> acc, String original) {
        XRange x = tryParseXRange(tok);
        if (x == null) {
            acc.add(Comparator.eq(parseVer(tok, original)));
            return;
        }
        switch (x.kind) {
            case ALL -> acc.add(Comparator.any());
            case MAJOR_ONLY -> {
                acc.add(Comparator.gte(Version.of(x.major, 0, 0)));
                acc.add(Comparator.lt(Version.of(Math.addExact(x.major, 1), 0, 0)));
            }
            case MAJOR_MINOR -> {
                acc.add(Comparator.gte(Version.of(x.major, x.minor, 0)));
                acc.add(Comparator.lt(Version.of(x.major, Math.addExact(x.minor, 1), 0)));
            }
        }
    }

    private enum XKind { ALL, MAJOR_ONLY, MAJOR_MINOR }

    private static final class XRange {
        final XKind kind;
        final int major;
        final int minor;
        XRange(XKind kind, int major, int minor) { this.kind = kind; this.major = major; this.minor = minor; }
    }

    /** @return null, если {@code tok} — не X-range. */
    private static XRange tryParseXRange(String tok) {
        if (tok.isEmpty()) return null;
        // Полный wildcard
        if ("*".equals(tok) || "x".equals(tok) || "X".equals(tok)) {
            return new XRange(XKind.ALL, 0, 0);
        }
        String[] parts = tok.split("\\.", -1);
        if (parts.length < 1 || parts.length > 3) return null;

        boolean foundWild = false;
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (isWildcard(p)) {
                foundWild = true;
            } else if (foundWild) {
                // После wildcard не должно быть конкретных чисел: "1.x.2" запрещён.
                return null;
            } else if (!isAllDigitsStatic(p)) {
                return null;
            }
        }
        if (!foundWild) return null;

        try {
            if (parts.length == 1 || isWildcard(parts[0])) {
                return new XRange(XKind.ALL, 0, 0);
            }
            int major = Integer.parseInt(parts[0]);
            if (parts.length == 2 || (parts.length == 3 && isWildcard(parts[1]))) {
                return new XRange(XKind.MAJOR_ONLY, major, 0);
            }
            int minor = Integer.parseInt(parts[1]);
            return new XRange(XKind.MAJOR_MINOR, major, minor);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean isWildcard(String s) {
        return "*".equals(s) || "x".equals(s) || "X".equals(s);
    }

    private static boolean isAllDigitsStatic(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private static Version parseVer(String s, String original) {
        if (s.isEmpty()) {
            throw new IllegalArgumentException("empty version in range '" + original + "'");
        }
        // Допускаем сокращения "1" и "1.2" как "1.0.0" и "1.2.0".
        if (!s.contains(".")) {
            return Version.parse(s + ".0.0");
        }
        if (countDots(s) == 1) {
            // Найдём, где начинается pre/build, чтобы не сломать идентификаторы.
            int splitIdx = firstIndexOf(s, '-', '+');
            String core = splitIdx < 0 ? s : s.substring(0, splitIdx);
            String tail = splitIdx < 0 ? "" : s.substring(splitIdx);
            return Version.parse(core + ".0" + tail);
        }
        return Version.parse(s);
    }

    private static int countDots(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == '.') n++;
        return n;
    }

    private static int firstIndexOf(String s, char a, char b) {
        int ia = s.indexOf(a);
        int ib = s.indexOf(b);
        if (ia < 0) return ib;
        if (ib < 0) return ia;
        return Math.min(ia, ib);
    }

    private static Version caretUpperBound(Version v) {
        if (v.major() > 0) return Version.of(Math.addExact(v.major(), 1), 0, 0);
        if (v.minor() > 0) return Version.of(0, Math.addExact(v.minor(), 1), 0);
        return Version.of(0, 0, Math.addExact(v.patch(), 1));
    }

    private static Version tildeUpperBound(Version v) {
        return Version.of(v.major(), Math.addExact(v.minor(), 1), 0);
    }

    // ===================================================================
    //                            COMPARATOR
    // ===================================================================

    private static final class Comparator {
        /** Тип сравнения. {@link Kind#ANY} — всегда true. */
        enum Kind { ANY, EQ, LT, LTE, GT, GTE }

        final Kind kind;
        /** Опорная версия; {@code null} только для {@link Kind#ANY}. */
        final Version pivot;

        Comparator(Kind kind, Version pivot) {
            this.kind = kind;
            this.pivot = pivot;
        }

        static Comparator any() { return new Comparator(Kind.ANY, null); }
        static Comparator eq(Version v)  { return new Comparator(Kind.EQ,  Objects.requireNonNull(v)); }
        static Comparator lt(Version v)  { return new Comparator(Kind.LT,  Objects.requireNonNull(v)); }
        static Comparator lte(Version v) { return new Comparator(Kind.LTE, Objects.requireNonNull(v)); }
        static Comparator gt(Version v)  { return new Comparator(Kind.GT,  Objects.requireNonNull(v)); }
        static Comparator gte(Version v) { return new Comparator(Kind.GTE, Objects.requireNonNull(v)); }

        boolean test(Version v) {
            return switch (kind) {
                case ANY -> true;
                case EQ  -> v.compareTo(pivot) == 0;
                case LT  -> v.compareTo(pivot) <  0;
                case LTE -> v.compareTo(pivot) <= 0;
                case GT  -> v.compareTo(pivot) >  0;
                case GTE -> v.compareTo(pivot) >= 0;
            };
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Comparator other)) return false;
            return kind == other.kind && Objects.equals(pivot, other.pivot);
        }

        @Override public int hashCode() { return Objects.hash(kind, pivot); }

        @Override
        public String toString() {
            return switch (kind) {
                case ANY -> "*";
                case EQ  -> "=" + pivot;
                case LT  -> "<" + pivot;
                case LTE -> "<=" + pivot;
                case GT  -> ">" + pivot;
                case GTE -> ">=" + pivot;
            };
        }
    }

}
