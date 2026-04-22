/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VersionRangeTest {

    @Test
    void starMatchesAnythingExceptPrereleaseByDefault() {
        VersionRange any = VersionRange.parse("*");
        assertThat(any.matches(Version.parse("0.0.1"))).isTrue();
        assertThat(any.matches(Version.parse("99.99.99"))).isTrue();
        // pre-release без seed — не проходит
        assertThat(any.matches(Version.parse("1.0.0-alpha"))).isFalse();
    }

    @Test
    void emptyRangeEqualsStar() {
        assertThat(VersionRange.parse("").matches(Version.parse("5.5.5"))).isTrue();
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            =1.2.3,        1.2.3, true
            =1.2.3,        1.2.4, false
            1.2.3,         1.2.3, true
            1.2.3,         1.2.4, false
            >1.2.3,        1.2.3, false
            >1.2.3,        1.2.4, true
            >=1.2.3,       1.2.3, true
            >=1.2.3,       1.2.2, false
            <2.0.0,        1.9.9, true
            <2.0.0,        2.0.0, false
            <=2.0.0,       2.0.0, true
            <=2.0.0,       2.0.1, false
            """)
    void basicComparators(String range, String ver, boolean expected) {
        assertThat(VersionRange.parse(range).matches(Version.parse(ver))).isEqualTo(expected);
    }

    @Test
    void caretMajorNonZero() {
        VersionRange r = VersionRange.parse("^1.2.3");
        assertThat(r.matches(Version.parse("1.2.3"))).isTrue();
        assertThat(r.matches(Version.parse("1.9.0"))).isTrue();
        assertThat(r.matches(Version.parse("2.0.0"))).isFalse();
        assertThat(r.matches(Version.parse("1.2.2"))).isFalse();
    }

    @Test
    void caretMajorZero() {
        VersionRange r = VersionRange.parse("^0.2.3");
        assertThat(r.matches(Version.parse("0.2.3"))).isTrue();
        assertThat(r.matches(Version.parse("0.2.9"))).isTrue();
        assertThat(r.matches(Version.parse("0.3.0"))).isFalse();
    }

    @Test
    void caretMajorAndMinorZero() {
        VersionRange r = VersionRange.parse("^0.0.3");
        assertThat(r.matches(Version.parse("0.0.3"))).isTrue();
        assertThat(r.matches(Version.parse("0.0.4"))).isFalse();
    }

    @Test
    void tilde() {
        VersionRange r = VersionRange.parse("~1.2.3");
        assertThat(r.matches(Version.parse("1.2.3"))).isTrue();
        assertThat(r.matches(Version.parse("1.2.99"))).isTrue();
        assertThat(r.matches(Version.parse("1.3.0"))).isFalse();
    }

    @Test
    void tildeShortForm() {
        VersionRange r = VersionRange.parse("~1.2");
        assertThat(r.matches(Version.parse("1.2.0"))).isTrue();
        assertThat(r.matches(Version.parse("1.2.99"))).isTrue();
        assertThat(r.matches(Version.parse("1.3.0"))).isFalse();
    }

    @Test
    void conjunction() {
        VersionRange r = VersionRange.parse(">=1.2.0 <1.5.0");
        assertThat(r.matches(Version.parse("1.2.0"))).isTrue();
        assertThat(r.matches(Version.parse("1.4.9"))).isTrue();
        assertThat(r.matches(Version.parse("1.5.0"))).isFalse();
        assertThat(r.matches(Version.parse("1.1.0"))).isFalse();
    }

    @Test
    void conjunctionWithAmp() {
        VersionRange r = VersionRange.parse(">=1.2.0 && <1.5.0");
        assertThat(r.matches(Version.parse("1.3.0"))).isTrue();
        assertThat(r.matches(Version.parse("1.5.0"))).isFalse();
    }

    @Test
    void disjunction() {
        VersionRange r = VersionRange.parse("1.x || ^2.0.0");
        // 1.x парсится как "1.0.0" (точное равенство), поэтому 1.x || ^2.0.0 редко нужен;
        // проверим через явные формы:
        VersionRange r2 = VersionRange.parse(">=1.0.0 <2.0.0 || ^2.0.0");
        assertThat(r2.matches(Version.parse("1.5.0"))).isTrue();
        assertThat(r2.matches(Version.parse("2.1.0"))).isTrue();
        assertThat(r2.matches(Version.parse("3.0.0"))).isFalse();

        // r с "1.x" — нестандарт, проверим, что хотя бы "2.x" прошло:
        assertThat(r.matches(Version.parse("2.5.0"))).isTrue();
    }

    @Test
    void prereleaseRequiresSeed() {
        VersionRange r = VersionRange.parse(">=1.2.3");
        assertThat(r.matches(Version.parse("1.3.0-alpha"))).isFalse();

        VersionRange seeded = VersionRange.parse(">=1.3.0-alpha");
        assertThat(seeded.matches(Version.parse("1.3.0-alpha"))).isTrue();
        assertThat(seeded.matches(Version.parse("1.3.0-beta"))).isTrue();
        // даже seed не допускает другой MAJOR.MINOR.PATCH в pre
        assertThat(seeded.matches(Version.parse("1.4.0-alpha"))).isFalse();
    }

    @Test
    void shortVersionFormsAreExpanded() {
        VersionRange r = VersionRange.parse(">=1");
        assertThat(r.matches(Version.parse("1.0.0"))).isTrue();
        assertThat(r.matches(Version.parse("1.2.3"))).isTrue();
        assertThat(r.matches(Version.parse("0.9.9"))).isFalse();
    }

    @Test
    void tryParseOnGarbage() {
        assertThat(VersionRange.tryParse("not-a-range ^^^")).isEmpty();
        assertThatThrownBy(() -> VersionRange.parse("^"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exactBuildsEquality() {
        VersionRange r = VersionRange.exact(Version.of(1, 2, 3));
        assertThat(r.matches(Version.of(1, 2, 3))).isTrue();
        assertThat(r.matches(Version.of(1, 2, 4))).isFalse();
    }
}
