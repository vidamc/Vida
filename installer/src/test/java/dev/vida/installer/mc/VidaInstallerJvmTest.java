/*
 * Copyright 2026 The Vida Project Authors.
 * Licensed under the Apache License, Version 2.0.
 */
package dev.vida.installer.mc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class VidaInstallerJvmTest {

    @Test
    void supported_range_legacy_and_calendar() {
        assertThat(VidaInstallerJvm.isSupportedGameVersion("1.21.0")).isFalse();
        assertThat(VidaInstallerJvm.isSupportedGameVersion("1.21.1")).isTrue();
        assertThat(VidaInstallerJvm.isSupportedGameVersion("1.21.24")).isTrue();
        assertThat(VidaInstallerJvm.isSupportedGameVersion("26.1.0")).isTrue();
        assertThat(VidaInstallerJvm.isSupportedGameVersion("26.1.2")).isTrue();
        assertThat(VidaInstallerJvm.isSupportedGameVersion("26.1.3")).isFalse();
        assertThat(VidaInstallerJvm.isSupportedGameVersion("26.1.preview")).isTrue();
        assertThat(VidaInstallerJvm.isSupportedGameVersion("1.22.1")).isFalse();
    }

    @Test
    void infer_platform_profile_ids() {
        assertThat(VidaInstallerJvm.inferPlatformProfileId("1.21.7"))
                .isEqualTo("legacy-121/1.21.7");
        assertThat(VidaInstallerJvm.inferPlatformProfileId("26.1.2"))
                .isEqualTo("calendar-26/26.1.2");
        assertThat(VidaInstallerJvm.inferPlatformProfileId("26.1.preview"))
                .isEqualTo("calendar-26/26.1.preview");
    }

    @Test
    void strip_managed_tokens() {
        String in = "-Xmx4G -Dvida.minecraftVersion=9.9.9 -javaagent:x.jar -Dvida.platformProfile=foo";
        assertThat(VidaInstallerJvm.stripManagedInstallerJvmTokens(in))
                .isEqualTo("-Xmx4G -javaagent:x.jar");
    }
}
