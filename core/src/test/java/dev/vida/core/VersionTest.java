/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class VersionTest {

    @Test
    void parsesBareCore() {
        Version v = Version.parse("1.2.3");
        assertThat(v.major()).isEqualTo(1);
        assertThat(v.minor()).isEqualTo(2);
        assertThat(v.patch()).isEqualTo(3);
        assertThat(v.isPreRelease()).isFalse();
        assertThat(v.preRelease()).isEmpty();
        assertThat(v.buildMeta()).isEmpty();
        assertThat(v.toString()).isEqualTo("1.2.3");
    }

    @Test
    void parsesPreRelease() {
        Version v = Version.parse("1.2.3-alpha.1");
        assertThat(v.isPreRelease()).isTrue();
        assertThat(v.preRelease()).containsExactly("alpha", "1");
        assertThat(v.toString()).isEqualTo("1.2.3-alpha.1");
    }

    @Test
    void parsesBuildMetadata() {
        Version v = Version.parse("1.0.0+mc1.21.1");
        assertThat(v.buildMeta()).containsExactly("mc1", "21", "1");
        // build не участвует в equals, но в toString присутствует
        assertThat(v).isEqualTo(Version.parse("1.0.0+whatever"));
        assertThat(v.toString()).isEqualTo("1.0.0+mc1.21.1");
    }

    @Test
    void parsesPreAndBuild() {
        Version v = Version.parse("1.0.0-rc.1+build.5");
        assertThat(v.preRelease()).isEqualTo(List.of("rc", "1"));
        assertThat(v.buildMeta()).isEqualTo(List.of("build", "5"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "1",
            "1.2",
            "1.2.3.4",
            "01.2.3",
            "1.02.3",
            "1.2.03",
            "1.2.3-",
            "1.2.3+",
            "1.2.3-alpha..1",
            "1.2.3-01",
            "1.2.3-alpha_1",
    })
    void rejectsInvalid(String raw) {
        assertThatThrownBy(() -> Version.parse(raw))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(Version.tryParse(raw)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            1.0.0,        1.0.0,        0
            1.0.0,        2.0.0,        -1
            2.0.0,        1.9.9,        1
            1.0.0,        1.1.0,        -1
            1.0.0,        1.0.1,        -1
            1.0.0-alpha,  1.0.0,        -1
            1.0.0,        1.0.0-alpha,  1
            1.0.0-alpha,  1.0.0-alpha.1, -1
            1.0.0-alpha.1, 1.0.0-alpha.beta, -1
            1.0.0-alpha.beta, 1.0.0-beta, -1
            1.0.0-beta,   1.0.0-beta.2, -1
            1.0.0-beta.2, 1.0.0-beta.11, -1
            1.0.0-beta.11, 1.0.0-rc.1, -1
            1.0.0-rc.1,   1.0.0,        -1
            """)
    void comparesPerSemverSpec(String aRaw, String bRaw, int expectedSign) {
        Version a = Version.parse(aRaw);
        Version b = Version.parse(bRaw);
        int actual = Integer.signum(a.compareTo(b));
        assertThat(actual).isEqualTo(expectedSign);
    }

    @Test
    void buildMetadataIgnoredInEquality() {
        Version a = Version.parse("1.0.0+build.1");
        Version b = Version.parse("1.0.0+build.99");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.compareTo(b)).isZero();
    }

    @Test
    void bumpers() {
        Version v = Version.of(1, 2, 3);
        assertThat(v.bumpPatch()).isEqualTo(Version.of(1, 2, 4));
        assertThat(v.bumpMinor()).isEqualTo(Version.of(1, 3, 0));
        assertThat(v.bumpMajor()).isEqualTo(Version.of(2, 0, 0));
    }

    @Test
    void stableCoreStripsPreAndBuild() {
        Version v = Version.parse("1.2.3-alpha+build.1");
        assertThat(v.stableCore()).isEqualTo(Version.of(1, 2, 3));
    }
}
